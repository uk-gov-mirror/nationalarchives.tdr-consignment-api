package uk.gov.nationalarchives.tdr.api.model.consignment

import java.io.IOException

import uk.gov.nationalarchives.oci.Alphabet.Alphabet
import uk.gov.nationalarchives.oci.{Alphabet, BaseCoder, IncludedAlphabet}

object ConsignmentReference {
  val alphabet: Either[IOException, Alphabet] = Alphabet.loadAlphabet(Right(IncludedAlphabet.GCRb25))

  def createConsignmentReference(transferStartYear: Int, consignmentSequenceId: Long): String = {
    val baseNumber = 25
    val encoded = alphabet match {
      case Left(error) => throw new Exception(s"Theres been an error creating the consignment reference", error)
      case Right(alphabet) =>
        val alphabetIndices = BaseCoder.encode(consignmentSequenceId, baseNumber)
        Alphabet.toString(alphabet, alphabetIndices)
    }
    s"TDR-$transferStartYear-$encoded"
  }
}
