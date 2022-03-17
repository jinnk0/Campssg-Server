package com.campssg.service;

import com.campssg.DB.entity.Mart;
import com.campssg.DB.entity.Product;
import com.campssg.DB.entity.User;
import com.campssg.DB.repository.MartRepository;
import com.campssg.DB.repository.ProductRepository;
import com.campssg.common.OpenApi;
import com.campssg.common.S3Uploder;
import com.campssg.dto.mart.MartCertificationRequestDto;
import com.campssg.dto.mart.MartListResponseDto;
import com.campssg.dto.mart.MartSaveRequestDto;
import com.campssg.dto.mart.ProductListResponse;
import com.campssg.dto.mart.ProductSaveRequest;
import com.campssg.util.SecurityUtil;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@AllArgsConstructor
public class MartService {

    private final MartRepository martRepository;

    private final ProductRepository productRepository;

    private final OpenApi openApi;

    private final S3Uploder s3Uploder;

    public void saveMart(MartSaveRequestDto requestDto) {
        boolean isValidate = openApi.martValidationOpenApi(
            MartCertificationRequestDto.builder()
                .bNo(requestDto.getBNo())
                //TODO: 로그인 한 사용자 이름 받아오기
                .pNm(((User) SecurityContextHolder.getContext().getAuthentication()
                    .getPrincipal()).getUserName())
                .startDt(requestDto.getStartDt())
                .build());
        if (isValidate) {
            martRepository.save(requestDto.toEntity(requestDto.getUserId()));
        } else {
            throw new IllegalArgumentException("마트 인증에 실패하였습니다.");
        }
    }

    public List<MartListResponseDto> findByUserId(Long userId) {
        List<Mart> martList = martRepository.findByUser_userId(userId);
        return martList.stream().map(mart -> new MartListResponseDto(mart)).collect(Collectors.toList());
    }

    public void saveProductToMart(ProductSaveRequest requestDto, MultipartFile file) throws IOException {
        String imgUrl = file == null ? null : s3Uploder.upload(file, "product");
        requestDto.setProductImgUrl(imgUrl);
        productRepository.save(requestDto.toEntity());
    }

    public ProductListResponse findProductByMartId(Long martId) {
        List<Product> productListByMart = productRepository.findByMart_martId(martId);

        List<ProductListResponse.ProductList> productLists = productListByMart.stream()
            .map(product -> new ProductListResponse().new ProductList(product)).collect(Collectors.toList());
        return new ProductListResponse(productLists);
    }
}
