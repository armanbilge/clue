// Copyright (c) 2016-2022 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package clue.model.json

import clue.model.GraphQLError
import clue.model.arb._
import io.circe.testing.instances._
import io.circe.testing.CodecTests

import munit.DisciplineSuite

final class GraphQLErrorJsonSpec extends DisciplineSuite {

  import ArbGraphQLError._

  checkAll("GraphQLError.PathElement", CodecTests[GraphQLError.PathElement].codec)
  checkAll("GraphQLError.Location", CodecTests[GraphQLError.Location].codec)
  checkAll("GraphQLError", CodecTests[GraphQLError].codec)

}
