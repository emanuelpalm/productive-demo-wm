package wm_demo.buyer;

import se.arkalix.dto.DtoReadableAs;

import static se.arkalix.dto.DtoEncoding.JSON;

@DtoReadableAs(JSON)
public interface Offer {
    boolean drilled();
    boolean milled();
    int quantity();
    double pricePerUnit();
}
