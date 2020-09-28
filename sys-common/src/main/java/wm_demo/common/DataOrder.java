package wm_demo.common;

import se.arkalix.dto.DtoReadableAs;
import se.arkalix.dto.DtoToString;
import se.arkalix.dto.DtoWritableAs;

import static se.arkalix.dto.DtoEncoding.JSON;

@DtoReadableAs(JSON)
@DtoWritableAs(JSON)
@DtoToString
public interface DataOrder {
    int quantity();
    String articleId();

    default DataOrderBuilder rebuild() {
        return new DataOrderBuilder()
            .quantity(quantity())
            .articleId(articleId());
    }
}
