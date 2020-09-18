package wm_demo.buyer;

import se.arkalix.dto.DtoWritableAs;

import java.util.Optional;

import static se.arkalix.dto.DtoEncoding.JSON;

@DtoWritableAs(JSON)
public interface DataOffer {
    long id();

    boolean drilled();

    boolean milled();

    int quantity();

    double pricePerUnit();

    Status status();

    Optional<DataOfferNew> counterOffer();

    enum Status {
        PENDING,
        ACCEPTED,
        COUNTERED,
        REJECTED,
        EXPIRED,
        FAILED,
    }

    default DataOfferBuilder rebuild() {
        return new DataOfferBuilder()
            .id(id())
            .drilled(drilled())
            .milled(milled())
            .quantity(quantity())
            .pricePerUnit(pricePerUnit())
            .status(status())
            .counterOffer((DataOfferNewDto) counterOffer().orElse(null));
    }
}
