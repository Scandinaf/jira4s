package com.allantl.jira4s.auth.jwt

import java.net.URI
import java.time.Duration
import java.time.temporal.ChronoUnit

import com.allantl.jira4s.auth.{AcJwtConfig, AuthContext}
import com.allantl.jira4s.auth.jwt.domain.CanonicalURIHttpRequest
import com.allantl.jira4s.auth.jwt.errors._
import io.toolsplus.atlassian.jwt.{HttpRequestCanonicalizer, JwtBuilder}

import scala.util.Try

object JwtGenerator {

  def generateToken(httpMethod: String, uri: String)(
      implicit acContext: AuthContext,
      acConfig: AcJwtConfig
  ): Either[JwtGeneratorError, String] =
    for {
      _ <- isSecretKeyLessThan256Bits(acContext)
      uri <- toJavaUri(uri)
      hostUri <- toJavaUri(acContext.instanceUrl)
      _ <- isAbsoluteUri(uri)
      _ <- isRequestToHost(uri, hostUri)
      token <- createToken(httpMethod, uri)
    } yield token

  private def createToken(httpMethod: String, uri: URI)(
      implicit acContext: AuthContext,
      acConfig: AcJwtConfig
  ): Either[JwtGeneratorError, String] = {
    val canonicalHttpRequest = CanonicalURIHttpRequest(httpMethod, uri)
    val queryHash = HttpRequestCanonicalizer.computeCanonicalRequestHash(canonicalHttpRequest)
    val expireAfter = Duration.of(acConfig.jwtExpirationInSeconds, ChronoUnit.SECONDS)

    new JwtBuilder(expireAfter)
      .withIssuer(acConfig.addOnKey)
      .withQueryHash(queryHash)
      .build(acContext.accessToken)
      .left
      .map(_ => InvalidSigningError)
  }

  private def isSecretKeyLessThan256Bits(
      implicit authContext: AuthContext): Either[JwtGeneratorError, Unit] =
    if (authContext.accessToken.getBytes.length < (256 / 8)) Left(InvalidSecretKey)
    else Right(())

  private def toJavaUri(str: String): Either[JwtGeneratorError, URI] =
    Try(new URI(str)).toOption.toRight(InvalidUriError)

  private def isAbsoluteUri(uri: URI): Either[JwtGeneratorError, URI] =
    if (uri.isAbsolute) Right(uri) else Left(RelativeUriError)

  private def isRequestToHost(uri: URI, hostUri: URI): Either[JwtGeneratorError, URI] = {
    val isReqToHost = !hostUri.relativize(uri).isAbsolute
    if (isReqToHost) Right(uri) else Left(BaseUrlMismatchError)
  }

}
