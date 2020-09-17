package wm_demo.buyer;

import se.arkalix.dto.DtoWritableAs;

import static se.arkalix.dto.DtoEncoding.JSON;

@DtoWritableAs(JSON)
public interface Order {
    int quantity();
    String articleId();
}
