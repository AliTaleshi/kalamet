package com.kalamet.user.dto;

import com.kalamet.user.domain.Address;
import com.kalamet.user.domain.Province;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Address and province bodies. */
public final class AddressDtos {

    private AddressDtos() {
    }

    public record ProvinceResponse(Short id, String name) {

        public static ProvinceResponse of(Province province) {
            return new ProvinceResponse(province.getId(), province.getName());
        }
    }

    public record AddressRequest(
            @NotBlank(message = "نام گیرنده را وارد کنید.") @Size(max = 200) String recipientName,
            @NotBlank(message = "شماره موبایل گیرنده را وارد کنید.") String recipientMobile,
            @NotNull(message = "استان را انتخاب کنید.") Short provinceId,
            @NotBlank(message = "شهر را وارد کنید.") @Size(max = 100) String city,
            @NotBlank(message = "نشانی پستی را وارد کنید.") @Size(max = 1000) String addressLine,
            @NotBlank(message = "پلاک را وارد کنید.") @Size(max = 20) String plaque,
            @Size(max = 20) String unit,
            @NotBlank(message = "کد پستی را وارد کنید.") String postalCode,
            Boolean makeDefault) {

        public boolean wantsDefault() {
            return Boolean.TRUE.equals(makeDefault);
        }
    }

    public record AddressResponse(Long id, String recipientName, String recipientMobile, ProvinceResponse province,
                                  String city, String addressLine, String plaque, String unit, String postalCode,
                                  boolean isDefault) {

        public static AddressResponse of(Address address) {
            return new AddressResponse(address.getId(), address.getRecipientName(), address.getRecipientMobile(),
                    ProvinceResponse.of(address.getProvince()), address.getCity(), address.getAddressLine(),
                    address.getPlaque(), address.getUnit(), address.getPostalCode(), address.isDefaultAddress());
        }
    }
}
