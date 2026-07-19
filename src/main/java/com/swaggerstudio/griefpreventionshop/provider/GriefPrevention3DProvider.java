package com.swaggerstudio.griefpreventionshop.provider;

/**
 * Claim provider backed by GriefPrevention3D — the actively-maintained, Folia-compatible fork.
 *
 * <p>GP3D shares GriefPrevention's API, so the behaviour is inherited from
 * {@link AbstractGpClaimProvider}. Folia-safe scheduling of writes is handled by the caller
 * (see {@code ClaimManager}) so it applies uniformly regardless of which provider is active.</p>
 */
public class GriefPrevention3DProvider extends AbstractGpClaimProvider {

    @Override
    protected String providerName() {
        return "GriefPrevention3D";
    }
}
