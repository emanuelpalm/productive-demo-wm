package wm_demo.common;

import se.arkalix.dto.DtoReadableAs;
import se.arkalix.dto.DtoToString;
import se.arkalix.dto.DtoWritableAs;

import java.time.Instant;
import java.util.Optional;

import static se.arkalix.dto.DtoEncoding.JSON;

@DtoReadableAs(JSON)
@DtoWritableAs(JSON)
@DtoToString
public interface DataOfferWithContext {
    long id();

    Instant timestamp();

    boolean drilled();

    boolean milled();

    int quantity();

    double pricePerUnit();

    DataOfferStatus status();

    Optional<DataOffer> counterOffer();

    default DataOfferWithContextBuilder rebuild() {
        return new DataOfferWithContextBuilder()
            .id(id())
            .timestamp(timestamp())
            .drilled(drilled())
            .milled(milled())
            .quantity(quantity())
            .pricePerUnit(pricePerUnit())
            .status(status())
            .counterOffer((DataOfferDto) counterOffer().orElse(null));
    }
}
