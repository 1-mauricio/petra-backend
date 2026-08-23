package com.marmorarias.orders.application;

import com.marmorarias.orders.domain.OrderState;

public record OrderTransitionView(OrderState toState, boolean allowed, String motivoBloqueio) {
}
