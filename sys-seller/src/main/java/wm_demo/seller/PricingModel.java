package wm_demo.seller;

import java.util.Objects;
import java.util.Optional;

public class PricingModel {
    private final int quantityMin;
    private final int quantityMax;
    private final double minUnitPrice;
    private final double quantityPriceMultiplier;
    private final double drilledPriceMultiplier;
    private final double milledPriceMultiplier;

    public PricingModel(Builder builder) {
        quantityMin = Objects.requireNonNullElse(builder.quantityMin, 1);
        quantityMax = Objects.requireNonNullElse(builder.quantityMax, Integer.MAX_VALUE);
        minUnitPrice = Objects.requireNonNull(builder.minUnitPrice);
        quantityPriceMultiplier = Objects.requireNonNullElse(builder.quantityPriceMultiplier, 1.0);
        drilledPriceMultiplier = Objects.requireNonNullElse(builder.drilledPriceMultiplier, 1.0);
        milledPriceMultiplier = Objects.requireNonNullElse(builder.milledPriceMultiplier, 1.0);
    }

    public Optional<Offer> getCounterOfferIfNotAcceptable(final Offer offer) {
        final var quantity = Math.min(quantityMax, Math.max(quantityMin, offer.quantity()));
        final var ppu = quantity
            * quantityPriceMultiplier
            * minUnitPrice
            * (offer.drilled() ? drilledPriceMultiplier : 1.0)
            * (offer.milled() ? milledPriceMultiplier : 1.0);

        validation:
        {
            if (offer.pricePerUnit() < ppu) {
                break validation;
            }
            if (offer.quantity() < quantityMin) {
                break validation;
            }
            if (offer.quantity() > quantityMax) {
                break validation;
            }
            return Optional.empty();
        }
        return Optional.of(offer.rebuild()
            .quantity(quantity)
            .pricePerUnit(ppu)
            .build());
    }

    public static class Builder {
        private Integer quantityMin;
        private Integer quantityMax;
        private Double quantityPriceMultiplier;
        private Double minUnitPrice;
        private Double drilledPriceMultiplier;
        private Double milledPriceMultiplier;

        public Builder quantityMin(final int quantityMin) {
            this.quantityMin = quantityMin;
            return this;
        }

        public Builder quantityMax(final int quantityMax) {
            this.quantityMax = quantityMax;
            return this;
        }

        public Builder quantityPriceMultiplier(final double quantityPriceMultiplier) {
            this.quantityPriceMultiplier = quantityPriceMultiplier;
            return this;
        }

        public Builder minUnitPrice(final double minUnitPrice) {
            this.minUnitPrice = minUnitPrice;
            return this;
        }

        public Builder drilledPriceMultiplier(final double drilledPriceMultiplier) {
            this.drilledPriceMultiplier = drilledPriceMultiplier;
            return this;
        }

        public Builder milledPriceMultiplier(final double milledPriceMultiplier) {
            this.milledPriceMultiplier = milledPriceMultiplier;
            return this;
        }

        public PricingModel build() {
            return new PricingModel(this);
        }
    }
}
