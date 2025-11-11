package com.asterexcrisys.adblocker.dispatchers;

import com.asterexcrisys.adblocker.models.types.DispatchType;
import com.asterexcrisys.adblocker.models.types.ProxyMode;

@SuppressWarnings("unused")
public sealed interface Dispatcher permits DefaultDispatcher, UDPDispatcher, TCPDispatcher, HTTPDispatcher {

    ProxyMode mode();

    DispatchType dispatch();

}