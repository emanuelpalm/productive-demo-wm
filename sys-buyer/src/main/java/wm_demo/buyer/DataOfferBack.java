package wm_demo.buyer;

import se.arkalix.dto.DtoWritableAs;

import java.util.Optional;

import static se.arkalix.dto.DtoEncoding.JSON;

@DtoWritableAs(JSON)
public interface DataOfferBack {
    int id();

    boolean drilled();

    boolean milled();

    int quantity();

    double pricePerUnit();

    Status status();

    Optional<DataOfferBack> counterOffer();

    enum Status {
        ACCEPTED,
        COUNTERED,
        REJECTED,
        PENDING,
    }
}
