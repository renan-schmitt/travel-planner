package com.travelplanner.places.domain;

import java.util.List;

/** Uma página de resultados da busca; {@code nextPageToken} nulo = última página. */
public record LodgingPage(List<LodgingPlace> places, String nextPageToken) {}
