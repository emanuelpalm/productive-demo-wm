package wm_demo.buyer;

import se.arkalix.dto.DtoReadableAs;
import se.arkalix.dto.DtoWritableAs;

import java.util.Optional;

import static se.arkalix.dto.DtoEncoding.JSON;

@DtoReadableAs(JSON)
@DtoWritableAs(JSON)
public interface DataOfferNew {
    Optional<Integer> id();

    boolean drilled();

    boolean milled();

    int quantity();

    double pricePerUnit();
}
