// Copyright (c) 2016-2021 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

// format: off
/*
  rules = [GraphQLGen]
  GraphQLGen.schemaDirs = ["gen/input/src/main/resources/graphql/schemas"]
 */
// format: on
package test

import clue.annotation.GraphQL
import clue.GraphQLOperation

@GraphQL
trait LucumaQueryGQL extends GraphQLOperation[LucumaODB] {
  val document = """
      query Program {
        program(programId: "p-2") {
          id
          name
          targets(first: 10, includeDeleted: true) {
            nodes {
              id
              name
              tracking {
                tracktype: __typename
                ... on Sidereal {
                  epoch
                }
                ... on Nonsidereal {
                  keyType
                }
              }
            }
          }
        }
      }"""
}