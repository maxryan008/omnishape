package dev.omnishape.api.facade;

import org.jetbrains.annotations.Nullable;

public interface FacadeHolder {

    @Nullable
    FacadeData omnishape$getFacade();

    void omnishape$setFacade(
            @Nullable FacadeData facade
    );

    default boolean omnishape$hasFacade() {
        return omnishape$getFacade() != null;
    }

    default void omnishape$clearFacade() {
        omnishape$setFacade(null);
    }
}