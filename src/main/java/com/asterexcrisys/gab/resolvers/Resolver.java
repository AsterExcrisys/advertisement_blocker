package com.asterexcrisys.gab.resolvers;

import org.xbill.DNS.Message;

public sealed interface Resolver permits STDResolver, SECResolver, DOTResolver, DOHResolver, DOQResolver {

    Message resolve(Message request);

}