package wm_demo.seller;

import se.arkalix.core.plugin.cp.SimplifiedContractCounterOffer;
import se.arkalix.core.plugin.cp.TrustedContractBuilder;
import se.arkalix.core.plugin.cp.TrustedContractNegotiation;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;

public class Offer {
    private final long id;
    private final boolean drilled;
    private final boolean milled;
    private final int quantity;
    private final double pricePerUnit;

    private Offer(final Builder builder) {
        this.id = Objects.requireNonNull(builder.id);
        this.drilled = Objects.requireNonNull(builder.drilled);
        this.milled = Objects.requireNonNull(builder.milled);
        this.quantity = Objects.requireNonNull(builder.quantity);
        this.pricePerUnit = Objects.requireNonNull(builder.pricePerUnit);
    }

    public static Offer fromNegotiation(final TrustedContractNegotiation negotiation) {
        final var id = negotiation.id();
        final var contracts = negotiation.offer().contracts();
        if (contracts.size() != 1) {
            throw new IllegalArgumentException("Expected exactly 1 contract");
        }
        final var contract = contracts.get(0);

        if (!contract.templateName().equals("component-order.txt")) {
            throw new IllegalArgumentException("Given contract is not a component order");
        }
        final var args = contract.arguments();

        final var quantityStr = args.get("quantity");
        if (quantityStr == null) {
            throw new IllegalArgumentException("No `quantity` is specified in given contract");
        }
        final var quantity = Integer.parseInt(quantityStr);

        final var articleId = args.get("articleId");
        if (articleId == null) {
            throw new IllegalArgumentException("No `articleId` specified in given contract");
        }
        if (!articleId.startsWith("ART-")) {
            throw new IllegalArgumentException("Expected `article-id` to start with `ART-`");
        }
        final var drilled = articleId.charAt(4) == 'D';
        final var milled = articleId.charAt(5) == 'M';

        final var unitPriceStr = args.get("unitPrice");
        if (unitPriceStr == null) {
            throw new IllegalArgumentException("No `unitPrice` specified in given contract");
        }
        final var unitPrice = Double.parseDouble(unitPriceStr);

        return new Offer.Builder()
            .id(id)
            .drilled(drilled)
            .milled(milled)
            .quantity(quantity)
            .pricePerUnit(unitPrice)
            .build();
    }

    public long id() {
        return id;
    }

    public boolean drilled() {
        return drilled;
    }

    public boolean milled() {
        return milled;
    }

    public int quantity() {
        return quantity;
    }

    public double pricePerUnit() {
        return pricePerUnit;
    }

    public Builder rebuild() {
        return new Builder()
            .id(id)
            .drilled(drilled)
            .milled(milled)
            .quantity(quantity)
            .pricePerUnit(pricePerUnit);
    }

    public SimplifiedContractCounterOffer toCounterOffer() {
        final var articleId = "ART-" +
            (drilled ? "D" : "P") +
            (milled ? "M" : "P");

        final var contract = new TrustedContractBuilder()
            .templateName("component-order.txt")
            .arguments(Map.of(
                "articleId", articleId,
                "quantity", "" + quantity,
                "unitPrice", "" + pricePerUnit
            ))
            .build();

        return new SimplifiedContractCounterOffer.Builder()
            .validFor(Duration.ofMinutes(3))
            .contracts(contract)
            .build();
    }

    public static class Builder {
        private Long id;
        private Boolean drilled;
        private Boolean milled;
        private Integer quantity;
        private Double pricePerUnit;

        public Builder id(final Long id) {
            this.id = id;
            return this;
        }

        public Builder drilled(final boolean drilled) {
            this.drilled = drilled;
            return this;
        }

        public Builder milled(final boolean milled) {
            this.milled = milled;
            return this;
        }

        public Builder quantity(final int quantity) {
            this.quantity = quantity;
            return this;
        }

        public Builder pricePerUnit(final double pricePerUnit) {
            this.pricePerUnit = pricePerUnit;
            return this;
        }

        public Offer build() {
            return new Offer(this);
        }
    }
}
