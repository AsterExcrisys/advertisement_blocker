package com.asterexcrisys.adblocker.resolvers;

import com.asterexcrisys.adblocker.models.types.ResolverType;
import org.xbill.DNS.Message;

@SuppressWarnings("unused")
public sealed interface Resolver extends AutoCloseable permits STDResolver, SECResolver, DOTResolver, DOQResolver, DOHResolver {

    ResolverType type();

    Message resolve(Message request);

}