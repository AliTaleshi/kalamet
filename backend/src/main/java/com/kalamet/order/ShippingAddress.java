package com.kalamet.order;

import com.kalamet.user.Address;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Copy of the delivery address taken at checkout, so editing an address never changes an order. */
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ShippingAddress {

    @Column(name = "ship_recipient_name", nullable = false, length = 200)
    private String recipientName;

    @Column(name = "ship_recipient_mobile", nullable = false, length = 11)
    private String recipientMobile;

    @Column(name = "ship_province", nullable = false, length = 50)
    private String province;

    @Column(name = "ship_city", nullable = false, length = 100)
    private String city;

    @Column(name = "ship_address_line", nullable = false)
    private String addressLine;

    @Column(name = "ship_plaque", nullable = false, length = 20)
    private String plaque;

    @Column(name = "ship_unit", length = 20)
    private String unit;

    @Column(name = "ship_postal_code", nullable = false, length = 10)
    private String postalCode;

    static ShippingAddress copyOf(Address address) {
        ShippingAddress copy = new ShippingAddress();
        copy.recipientName = address.getRecipientName();
        copy.recipientMobile = address.getRecipientMobile();
        copy.province = address.getProvince().getName();
        copy.city = address.getCity();
        copy.addressLine = address.getAddressLine();
        copy.plaque = address.getPlaque();
        copy.unit = address.getUnit();
        copy.postalCode = address.getPostalCode();
        return copy;
    }
}
