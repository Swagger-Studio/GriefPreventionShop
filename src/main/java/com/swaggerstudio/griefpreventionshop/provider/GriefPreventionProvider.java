package com.swaggerstudio.griefpreventionshop.provider;

/** Claim provider backed by the standard GriefPrevention plugin. */
public class GriefPreventionProvider extends AbstractGpClaimProvider {

    @Override
    protected String providerName() {
        return "GriefPrevention";
    }
}
