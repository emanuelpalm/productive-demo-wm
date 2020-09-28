package wm_demo.common;

import se.arkalix.core.plugin.cp.SimplifiedContractCounterOffer;
import se.arkalix.core.plugin.cp.TrustedContract;
import se.arkalix.core.plugin.cp.TrustedContractBuilder;
import se.arkalix.core.plugin.cp.TrustedContractDto;
import se.arkalix.dto.DtoReadableAs;
import se.arkalix.dto.DtoToString;
import se.arkalix.dto.DtoWritableAs;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

import static se.arkalix.dto.DtoEncoding.JSON;

@DtoReadableAs(JSON)
@DtoWritableAs(JSON)
@DtoToString
public interface DataOffer {
    Optional<Long> id();

    boolean drilled();

    boolean milled();

    int quantity();

    double pricePerUnit();

    default DataOfferBuilder rebuild() {
        return new DataOfferBuilder()
            .id(id().orElse(null))
            .drilled(drilled())
            .milled(milled())
            .quantity(quantity())
            .pricePerUnit(pricePerUnit());
    }

    default String articleId() {
        return "ART-" + (drilled() ? "D" : "P") + (milled() ? "M" : "P");
    }

    default TrustedContractDto toContract() {
        return new TrustedContractBuilder()
            .templateName("component-order.txt")
            .arguments(Map.of(
                "articleId", articleId(),
                "quantity", "" + quantity(),
                "unitPrice", "" + pricePerUnit()
            ))
            .build();
    }

    default SimplifiedContractCounterOffer toSimplifiedContractCounterOffer() {
        return new SimplifiedContractCounterOffer.Builder()
            .validFor(Duration.ofMinutes(3))
            .contracts(toContract())
            .build();
    }

    static DataOfferDto fromContract(final TrustedContract contract) {
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

        return new DataOfferBuilder()
            .drilled(drilled)
            .milled(milled)
            .quantity(quantity)
            .pricePerUnit(unitPrice)
            .build();
    }
}
