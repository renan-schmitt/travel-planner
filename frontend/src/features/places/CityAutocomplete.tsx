import { Loader2, MapPin, Search } from 'lucide-react'
import { useId, useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { NativeSelect } from '@/components/ui/native-select'
import { ApiError } from '@/lib/api'
import { countryFlag, countryOptions } from '@/lib/countries'
import { timeZoneLabel, timeZoneOptions } from '@/lib/timezones'
import { useDebouncedValue } from '@/lib/use-debounced-value'
import { cn } from '@/lib/utils'
import { fetchCity, MIN_QUERY, useCityAutocomplete, type CitySuggestion } from './api'
import { emptyCity, type CityValue } from './city'

type Mode = 'search' | 'selected' | 'manual'

/**
 * Campo de cidade com autocomplete do Google Places (debounce de 300 ms, um `sessionToken` por
 * busca) e fallback manual (nome + país + fuso) quando o Places está indisponível ou o usuário
 * prefere digitar. Controlado: `value` vazio (`name: ''`) significa "sem cidade".
 */
export function CityAutocomplete({
  label,
  value,
  onChange,
  error,
}: {
  label: string
  value: CityValue
  onChange: (city: CityValue) => void
  error?: string
}) {
  const [mode, setMode] = useState<Mode>(value.name ? 'selected' : 'search')
  const [unavailable, setUnavailable] = useState(false)

  if (mode === 'selected' && value.name) {
    return (
      <SelectedCity
        label={label}
        value={value}
        error={error}
        onChange={() => {
          onChange(emptyCity)
          setMode('search')
        }}
        onEdit={() => setMode('manual')}
      />
    )
  }
  if (mode === 'manual') {
    return (
      <ManualCity
        label={label}
        value={value}
        onChange={onChange}
        error={error}
        unavailable={unavailable}
        onSearch={() => {
          onChange(emptyCity)
          setUnavailable(false)
          setMode('search')
        }}
      />
    )
  }
  return (
    <CitySearch
      label={label}
      error={error}
      onSelect={(city) => {
        onChange(city)
        // Sem país ou fuso (raro), o usuário completa à mão.
        setMode(city.countryCode && city.timezone ? 'selected' : 'manual')
      }}
      onManual={(name, isUnavailable) => {
        onChange({ ...emptyCity, name })
        setUnavailable(isUnavailable)
        setMode('manual')
      }}
    />
  )
}

function CitySearch({
  label,
  error,
  onSelect,
  onManual,
}: {
  label: string
  error?: string
  onSelect: (city: CityValue) => void
  onManual: (typedName: string, unavailable: boolean) => void
}) {
  const { t } = useTranslation()
  const id = useId()
  const [session] = useState(() => crypto.randomUUID())
  const [query, setQuery] = useState('')
  const [active, setActive] = useState(-1)
  const [loadingPlace, setLoadingPlace] = useState<string>()
  const [detailsFailed, setDetailsFailed] = useState(false)
  const debounced = useDebouncedValue(query, 300)
  const search = useCityAutocomplete(debounced, session)

  const tooShort = query.trim().length < MIN_QUERY
  const suggestions = !tooShort && search.data ? search.data : []
  const unavailable = search.error instanceof ApiError && search.error.status === 503
  const listId = `${id}-list`

  async function select(suggestion: CitySuggestion) {
    setLoadingPlace(suggestion.placeId)
    setDetailsFailed(false)
    try {
      const city = await fetchCity(suggestion.placeId, session)
      onSelect({
        name: city.name,
        countryCode: city.countryCode ?? '',
        timezone: city.timezone ?? '',
        placeId: city.placeId,
        lat: city.lat,
        lng: city.lng,
      })
    } catch (e) {
      if (e instanceof ApiError && e.status === 503) onManual(suggestion.name, true)
      else setDetailsFailed(true)
    } finally {
      setLoadingPlace(undefined)
    }
  }

  function onKeyDown(event: React.KeyboardEvent<HTMLInputElement>) {
    if (suggestions.length === 0) return
    if (event.key === 'ArrowDown') {
      event.preventDefault()
      setActive((i) => (i + 1) % suggestions.length)
    } else if (event.key === 'ArrowUp') {
      event.preventDefault()
      setActive((i) => (i <= 0 ? suggestions.length - 1 : i - 1))
    } else if (event.key === 'Enter' && suggestions[active]) {
      event.preventDefault()
      void select(suggestions[active])
    }
  }

  return (
    <div className="flex flex-col gap-2">
      <Label htmlFor={id}>{label}</Label>
      <div className="relative">
        <Search
          className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-muted-foreground"
          strokeWidth={1.75}
          aria-hidden
        />
        <Input
          id={id}
          role="combobox"
          aria-expanded={suggestions.length > 0}
          aria-controls={listId}
          aria-autocomplete="list"
          aria-activedescendant={active >= 0 ? `${listId}-${active}` : undefined}
          aria-invalid={error ? true : undefined}
          autoComplete="off"
          placeholder={t('city.searchPlaceholder')}
          className="pl-9"
          value={query}
          onChange={(e) => {
            setQuery(e.target.value)
            setActive(-1)
          }}
          onKeyDown={onKeyDown}
        />
        {(search.isFetching || loadingPlace) && (
          <Loader2
            className="absolute top-1/2 right-3 size-4 -translate-y-1/2 animate-spin text-muted-foreground"
            aria-label={t('common.loading')}
          />
        )}
      </div>

      {suggestions.length > 0 && (
        <ul
          id={listId}
          role="listbox"
          aria-label={t('city.suggestions')}
          className="flex flex-col overflow-hidden rounded-lg border"
        >
          {suggestions.map((s, i) => (
            <li
              key={s.placeId}
              id={`${listId}-${i}`}
              role="option"
              aria-selected={i === active}
              className={cn(
                'flex min-h-11 cursor-pointer items-center gap-3 border-b px-3 py-2 last:border-b-0 hover:bg-accent',
                i === active && 'bg-accent',
                loadingPlace && 'pointer-events-none opacity-60',
              )}
              onClick={() => void select(s)}
            >
              <MapPin
                className="size-4 shrink-0 text-muted-foreground"
                strokeWidth={1.75}
                aria-hidden
              />
              <span className="min-w-0">
                <span className="block truncate font-medium">{s.name}</span>
                {s.description && (
                  <span className="block truncate text-sm text-muted-foreground">
                    {s.description}
                  </span>
                )}
              </span>
            </li>
          ))}
        </ul>
      )}

      <p className="text-sm text-muted-foreground" aria-live="polite">
        {unavailable
          ? t('city.unavailable')
          : detailsFailed
            ? t('city.detailsError')
            : !tooShort && search.isSuccess && suggestions.length === 0 && !search.isFetching
              ? t('city.noResults')
              : tooShort && query
                ? t('city.minChars', { count: MIN_QUERY })
                : ''}
      </p>
      {error && (
        <p className="text-sm text-destructive">{t(`city.${error}` as 'city.cityRequired')}</p>
      )}
      <Button
        type="button"
        variant={unavailable ? 'outline' : 'link'}
        className={cn('self-start', !unavailable && 'h-auto px-0')}
        onClick={() => onManual(query.trim(), unavailable)}
      >
        {t('city.typeManually')}
      </Button>
    </div>
  )
}

function SelectedCity({
  label,
  value,
  error,
  onChange,
  onEdit,
}: {
  label: string
  value: CityValue
  error?: string
  onChange: () => void
  onEdit: () => void
}) {
  const { t, i18n } = useTranslation()
  const labelId = useId()
  const country = useMemo(
    () =>
      value.countryCode
        ? new Intl.DisplayNames([i18n.language], { type: 'region' }).of(value.countryCode)
        : undefined,
    [i18n.language, value.countryCode],
  )

  return (
    <div role="group" aria-labelledby={labelId} className="flex flex-col gap-2">
      <p id={labelId} className="text-sm leading-none font-medium">
        {label}
      </p>
      <div className="flex items-center gap-3 rounded-lg border p-3">
        <span className="text-2xl leading-none" aria-hidden>
          {value.countryCode ? countryFlag(value.countryCode) : '📍'}
        </span>
        <div className="min-w-0 flex-1">
          <p className="truncate font-medium">{value.name}</p>
          <p className="truncate text-sm text-muted-foreground">
            {[country, value.timezone && timeZoneLabel(value.timezone)].filter(Boolean).join(' · ')}
          </p>
        </div>
        <div className="flex shrink-0 flex-col items-end">
          <Button type="button" variant="ghost" size="sm" onClick={onChange}>
            {t('city.change')}
          </Button>
          {!value.placeId && (
            <Button type="button" variant="ghost" size="sm" onClick={onEdit}>
              {t('city.edit')}
            </Button>
          )}
        </div>
      </div>
      {error && (
        <p className="text-sm text-destructive">{t(`city.${error}` as 'city.cityRequired')}</p>
      )}
    </div>
  )
}

function ManualCity({
  label,
  value,
  onChange,
  error,
  unavailable,
  onSearch,
}: {
  label: string
  value: CityValue
  onChange: (city: CityValue) => void
  error?: string
  unavailable: boolean
  onSearch: () => void
}) {
  const { t, i18n } = useTranslation()
  const id = useId()
  const countries = useMemo(() => countryOptions(i18n.language), [i18n.language])
  const zones = useMemo(() => timeZoneOptions(), [])
  // Cidade do Places sem país/fuso: só completa o que falta, mantendo placeId e coordenadas.
  const fromPlaces = Boolean(value.placeId)

  return (
    <fieldset className="flex flex-col gap-3">
      <legend className="mb-2 text-sm leading-none font-medium">{label}</legend>
      {unavailable && (
        <p className="text-sm text-muted-foreground">{t('city.unavailableManual')}</p>
      )}
      <div className="flex flex-col gap-2">
        <Label htmlFor={`${id}-name`}>{t('city.name')}</Label>
        <Input
          id={`${id}-name`}
          maxLength={160}
          autoComplete="address-level2"
          readOnly={fromPlaces}
          value={value.name}
          aria-invalid={error === 'cityRequired' ? true : undefined}
          onChange={(e) =>
            onChange({
              ...value,
              name: e.target.value,
              placeId: undefined,
              lat: undefined,
              lng: undefined,
            })
          }
        />
      </div>
      <div className="grid gap-3 sm:grid-cols-2">
        <div className="flex min-w-0 flex-col gap-2">
          <Label htmlFor={`${id}-country`}>{t('city.country')}</Label>
          <NativeSelect
            id={`${id}-country`}
            value={value.countryCode}
            aria-invalid={error === 'countryRequired' ? true : undefined}
            onChange={(e) => onChange({ ...value, countryCode: e.target.value })}
          >
            <option value="">{t('city.select')}</option>
            {countries.map(({ code, name, flag }) => (
              <option key={code} value={code}>
                {flag} {name}
              </option>
            ))}
          </NativeSelect>
        </div>
        <div className="flex min-w-0 flex-col gap-2">
          <Label htmlFor={`${id}-timezone`}>{t('city.timezone')}</Label>
          <NativeSelect
            id={`${id}-timezone`}
            value={value.timezone}
            aria-invalid={error === 'timezoneRequired' ? true : undefined}
            onChange={(e) => onChange({ ...value, timezone: e.target.value })}
          >
            <option value="">{t('city.select')}</option>
            {zones.map((zone) => (
              <option key={zone} value={zone}>
                {timeZoneLabel(zone)}
              </option>
            ))}
          </NativeSelect>
        </div>
      </div>
      {error && (
        <p className="text-sm text-destructive">{t(`city.${error}` as 'city.cityRequired')}</p>
      )}
      <Button type="button" variant="link" className="h-auto self-start px-0" onClick={onSearch}>
        {t('city.searchAgain')}
      </Button>
    </fieldset>
  )
}
