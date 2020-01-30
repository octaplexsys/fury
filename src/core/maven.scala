/*
   ╔═══════════════════════════════════════════════════════════════════════════════════════════════════════════╗
   ║ Fury, version 0.8.0. Copyright 2018-20 Jon Pretty, Propensive OÜ.                                         ║
   ║                                                                                                           ║
   ║ The primary distribution site is: https://propensive.com/                                                 ║
   ║                                                                                                           ║
   ║ Licensed under  the Apache License,  Version 2.0 (the  "License"); you  may not use  this file  except in ║
   ║ compliance with the License. You may obtain a copy of the License at                                      ║
   ║                                                                                                           ║
   ║     http://www.apache.org/licenses/LICENSE-2.0                                                            ║
   ║                                                                                                           ║
   ║ Unless required  by applicable law  or agreed to in  writing, software  distributed under the  License is ║
   ║ distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. ║
   ║ See the License for the specific language governing permissions and limitations under the License.        ║
   ╚═══════════════════════════════════════════════════════════════════════════════════════════════════════════╝
*/
package fury.core

import fury.model._, fury.io._, fury.strings._

import euphemism._

import scala.util._

object MavenCentral {
  case class Doc(g: String, a: String, v: String, id: String)

  def search(binSpec: PartialBinSpec)(implicit log: Log): Try[Set[String]] = {
    val query = binSpec match {
      case PartialBinSpec(g, None, None)        => str"""g:%22$g%22"""
      case PartialBinSpec(g, Some(""), None)    => str"""g:%22$g%22"""
      case PartialBinSpec(g, Some(a), None)     => str"""g:%22$g%22"""
      case PartialBinSpec(g, Some(a), Some("")) => str"""g:%22$g%22+AND+a:%22$a%22"""
      case PartialBinSpec(g, Some(a), Some(v))  => str"""g:%22$g%22+AND+a:%22$a%22"""
    }
    
    for {
      string <- Http.get(Https(Path("search.maven.org") / "solrsearch" / 
                     str"select?q=$query&core=gav&wt=json&rows=100"), Map(), Set())

      json   <- Try(Json.parse(new String(string, "UTF-8")).get)
      docs   <- Try(json.response.docs.as[Set[Doc]].get)
    } yield {
      binSpec match {
        case PartialBinSpec(_, None, None) =>
          docs.flatMap { r => Set(str"${r.g}:", str"${r.g}:_") }
        case PartialBinSpec(g, Some(a), None)  =>
          val prefix = str"$g:$a"
          docs.map { r => str"${r.g}:${r.a}:${r.v}" }.filter(_.startsWith(prefix))
        case PartialBinSpec(_, _, Some(v)) =>
          docs.filter(_.v startsWith v).map(_.id)
      }
    }
  }
}