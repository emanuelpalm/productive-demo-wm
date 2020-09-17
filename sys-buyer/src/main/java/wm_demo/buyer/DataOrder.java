package wm_demo.buyer;

import se.arkalix.dto.DtoWritableAs;

import static se.arkalix.dto.DtoEncoding.JSON;

@DtoWritableAs(JSON)
public interface DataOrder {
    int quantity();
    String articleId();
}
