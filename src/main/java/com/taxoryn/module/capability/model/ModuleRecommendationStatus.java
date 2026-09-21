package com.taxoryn.module.capability.model;

/**
 * Status indicating how a product capability relates to an organization persona
 * and their current subscription plan.
 */
public enum ModuleRecommendationStatus {
    /**
     * Active and enabled for this organization persona and subscription tier.
     */
    ACTIVE,

    /**
     * Recommended for this persona but optional / secondary priority.
     */
    RECOMMENDED,

    /**
     * Highly relevant or desirable for this persona, but requires a subscription plan upgrade.
     */
    UPGRADE_REQUIRED,

    /**
     * Not recommended or outside typical operational scope for this persona.
     */
    NOT_RECOMMENDED
}
