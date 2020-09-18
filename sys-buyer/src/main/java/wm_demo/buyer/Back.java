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
            public DataOfferDto[] onGetOffers() {
                return new DataOfferDto[]{
                    new DataOfferBuilder()
                        .id(207)
                        .drilled(true)
                        .milled(false)
                        .quantity(4)
                        .pricePerUnit(320.00)
                        .status(DataOffer.Status.PENDING)
                        .build(),
                    new DataOfferBuilder()
                        .id(193)
                        .drilled(true)
                        .milled(true)
                        .quantity(8)
                        .pricePerUnit(302.75)
                        .status(DataOffer.Status.ACCEPTED)
                        .build(),
                    new DataOfferBuilder()
                        .id(132)
                        .drilled(false)
                        .milled(false)
                        .quantity(30)
                        .pricePerUnit(104.40)
                        .status(DataOffer.Status.REJECTED)
                        .build(),
                    new DataOfferBuilder()
                        .id(107)
                        .drilled(true)
                        .milled(false)
                        .quantity(20)
                        .pricePerUnit(400.50)
                        .status(DataOffer.Status.COUNTERED)
                        .counterOffer(new DataOfferNewBuilder()
                            .id(107)
                            .drilled(true)
                            .milled(false)
                            .quantity(50)
                            .pricePerUnit(400.00)
                            .build())
                        .build(),
                    new DataOfferBuilder()
                        .id(32)
                        .drilled(true)
                        .milled(true)
                        .quantity(8)
                        .pricePerUnit(302.75)
                        .status(DataOffer.Status.ACCEPTED)
                        .build(),
                };
            }

            @Override
            public DataOrderDto[] onGetOrders() {
                return new DataOrderDto[]{
                    new DataOrderBuilder()
                        .articleId("ART-DM")
                        .quantity(4)
                        .build(),
                    new DataOrderBuilder()
                        .articleId("ART-DP")
                        .quantity(3)
                        .build(),
                    new DataOrderBuilder()
                        .articleId("ART-PP")
                        .quantity(12)
                        .build(),
                    new DataOrderBuilder()
                        .articleId("ART-PM")
                        .quantity(1)
                        .build(),
                };
            }

            @Override
            public Future<?> onOffer(DataOfferNewDto offer) {
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