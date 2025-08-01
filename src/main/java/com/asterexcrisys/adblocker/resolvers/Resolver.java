package com.asterexcrisys.adblocker.resolvers;

import org.xbill.DNS.Message;

@SuppressWarnings("unused")
public sealed interface Resolver permits STDResolver, SECResolver, DOTResolver, DOHResolver, DOQResolver {

    Message resolve(Message request);

}