package com.kalamet.user.service;

import com.kalamet.common.error.ApiException;
import com.kalamet.common.text.MobileNumber;
import com.kalamet.common.text.PersianText;
import com.kalamet.user.domain.Address;
import com.kalamet.user.dto.AddressDtos.AddressRequest;
import com.kalamet.user.dto.AddressDtos.AddressResponse;
import com.kalamet.user.dto.AddressDtos.ProvinceResponse;
import com.kalamet.user.repository.AddressRepository;
import com.kalamet.user.repository.ProvinceRepository;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AddressService {

    private static final int MAX_ADDRESSES = 20;

    private final AddressRepository addresses;
    private final ProvinceRepository provinces;
    private final UserService userService;

    AddressService(AddressRepository addresses, ProvinceRepository provinces, UserService userService) {
        this.addresses = addresses;
        this.provinces = provinces;
        this.userService = userService;
    }

    @Transactional(readOnly = true)
    public List<ProvinceResponse> provinces() {
        return provinces.findAll(Sort.by("name")).stream().map(ProvinceResponse::of).toList();
    }

    @Transactional(readOnly = true)
    public List<AddressResponse> list(Long userId) {
        return addresses.findByUserIdOrderByDefaultAddressDescCreatedAtDesc(userId).stream()
                .map(AddressResponse::of).toList();
    }

    /** The address entity, for checkout. */
    @Transactional(readOnly = true)
    public Address require(Long userId, Long addressId) {
        return addresses.findByIdAndUserId(addressId, userId).orElseThrow(AddressService::notFound);
    }

    @Transactional
    public AddressResponse create(Long userId, AddressRequest request) {
        long existing = addresses.countByUserId(userId);
        if (existing >= MAX_ADDRESSES) {
            throw ApiException.badRequest("ADDRESS_LIMIT", "حداکثر " + PersianText.digits(MAX_ADDRESSES) + " نشانی می‌توانید ثبت کنید.");
        }
        boolean makeDefault = request.wantsDefault() || existing == 0;
        if (makeDefault) {
            addresses.clearDefault(userId);
        }
        Address address = new Address(userService.require(userId));
        apply(address, request);
        address.setDefaultAddress(makeDefault);
        return AddressResponse.of(addresses.saveAndFlush(address));
    }

    @Transactional
    public AddressResponse update(Long userId, Long addressId, AddressRequest request) {
        if (request.wantsDefault()) {
            addresses.clearDefault(userId);
        }
        Address address = require(userId, addressId);
        apply(address, request);
        if (request.wantsDefault()) {
            address.setDefaultAddress(true);
        }
        return AddressResponse.of(addresses.saveAndFlush(address));
    }

    @Transactional
    public AddressResponse makeDefault(Long userId, Long addressId) {
        addresses.clearDefault(userId);
        Address address = require(userId, addressId);
        address.setDefaultAddress(true);
        return AddressResponse.of(addresses.saveAndFlush(address));
    }

    /** Deleting the default address promotes the most recent remaining one. */
    @Transactional
    public void delete(Long userId, Long addressId) {
        Address address = require(userId, addressId);
        boolean wasDefault = address.isDefaultAddress();
        addresses.delete(address);
        addresses.flush();
        if (wasDefault) {
            addresses.findByUserIdOrderByDefaultAddressDescCreatedAtDesc(userId).stream().findFirst()
                    .ifPresent(next -> next.setDefaultAddress(true));
        }
    }

    private void apply(Address address, AddressRequest request) {
        String postalCode = PersianText.asciiDigits(request.postalCode());
        if (postalCode == null || !postalCode.matches("^[0-9]{10}$")) {
            throw ApiException.badRequest("INVALID_POSTAL_CODE", "کد پستی باید ۱۰ رقم باشد.");
        }
        address.setRecipientName(PersianText.normalize(request.recipientName()));
        address.setRecipientMobile(MobileNumber.require(request.recipientMobile()));
        address.setProvince(provinces.findById(request.provinceId())
                .orElseThrow(() -> ApiException.badRequest("INVALID_PROVINCE", "استان انتخاب‌شده معتبر نیست.")));
        address.setCity(PersianText.normalize(request.city()));
        address.setAddressLine(PersianText.normalize(request.addressLine()));
        address.setPlaque(PersianText.normalize(PersianText.digitsToAscii(request.plaque())));
        address.setUnit(PersianText.normalize(PersianText.digitsToAscii(request.unit())));
        address.setPostalCode(postalCode);
    }

    private static ApiException notFound() {
        return ApiException.notFound("ADDRESS_NOT_FOUND", "نشانی پیدا نشد.");
    }
}
