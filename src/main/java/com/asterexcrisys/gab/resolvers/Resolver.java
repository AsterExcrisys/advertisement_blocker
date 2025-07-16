package com.asterexcrisys.aab.resolvers;

import org.xbill.DNS.Message;

public sealed interface Resolver permits STDResolver, SECResolver, DOTResolver, DOHResolver, DOQResolver {

    Message resolve(Message request);

}