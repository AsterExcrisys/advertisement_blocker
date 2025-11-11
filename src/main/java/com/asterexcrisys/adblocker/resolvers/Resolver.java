package com.asterexcrisys.adblocker.resolvers;

import org.xbill.DNS.Message;

@SuppressWarnings("unused")
public sealed interface Resolver extends AutoCloseable permits STDResolver, SECResolver, DOTResolver, DOQResolver, DOHResolver {

    Message resolve(Message request);

}