package com.allantl.jira4s.v2.client

import com.allantl.jira4s.auth.AuthContext
import com.allantl.jira4s.v2.domain.errors.JiraError
import com.softwaremill.sttp.circe.asJson
import com.softwaremill.sttp.{SttpBackend, sttp, _}
import io.circe.Decoder

private[jira4s] trait ResourceClient[R[_], T <: AuthContext] extends HasClient[R] {
  private implicit val be: SttpBackend[R, Nothing] = backend

  def getResourceJson[E: Decoder](
      path: String,
      headers: Map[String, String] = Map.empty,
  )(
      implicit userCtx: T
  ): R[Either[JiraError, E]] =
    sttp
      .get(
        uri"$restEndpoint"
          .path(path)
      )
      .headers(headers)
      .jiraAuthenticated
      .response(asJson[E])
      .send()
      .parseResponse

  def getResourceHtml(
      path: String,
      headers: Map[String, String] = Map.empty,
  )(
      implicit userCtx: T
  ): R[Either[JiraError, String]] =
    sttp
      .get(
        uri"$restEndpoint"
          .path(path)
      )
      .headers(headers)
      .jiraAuthenticated
      .send()
      .getContent
}
