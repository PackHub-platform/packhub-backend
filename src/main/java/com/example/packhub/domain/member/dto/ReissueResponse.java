package com.example.packhub.domain.member.dto;

/** 재발급하면 Access·Refresh 둘 다 새로 준다. 보낸 Refresh Token은 다시 쓸 수 없다 */
public record ReissueResponse(
	String accessToken,
	String refreshToken,
	String tokenType,
	long accessTokenExpiresIn,
	long refreshTokenExpiresIn
) {
}
