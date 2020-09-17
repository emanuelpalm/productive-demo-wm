package wm_demo.buyer;

import se.arkalix.util.concurrent.Future;

public class Back {
    private Back() {}

    public static Future<Back> createAndBindTo(final int port) {
        return Future.success(new Back());
    }

    public int port() {
        return 0;
    }

    public Front.RequestHandler handler() {
        return new Front.RequestHandler() {
            @Override
            public DataOrderDto[] onGetOrders() {
                return new DataOrderDto[0];
            }

            @Override
            public DataOfferBackDto[] onGetOffers() {
                return new DataOfferBackDto[0];
            }

            @Override
            public Future<?> onOffer(DataOfferUserDto offer) {
                return Future.done();
            }
        };
    }
}

/*
final var contract = new TrustedContractBuilder()
    .templateName("component-order.txt")
    .arguments(Map.of(
        "article-id", "ART-" +
            (offer.drilled() ? "D" : "P") +
            (offer.milled() ? "M" : "P"),
        "quantity", "" + offer.quantity(),
        "unit-price", "" + offer.pricePerUnit()
    ))
    .build();
 */