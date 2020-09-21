package wm_demo.buyer;

import se.arkalix.dto.DtoReadableAs;
import se.arkalix.dto.DtoWritableAs;

import static se.arkalix.dto.DtoEncoding.JSON;

@DtoReadableAs(JSON)
@DtoWritableAs(JSON)
public interface DataOrder {
    int quantity();
    String articleId();

    default DataOrderBuilder rebuild() {
        return new DataOrderBuilder()
            .quantity(quantity())
            .articleId(articleId());
    }
}
