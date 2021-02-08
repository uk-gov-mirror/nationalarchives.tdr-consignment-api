package uk.gov.nationalarchives.tdr.api.model.consignment

import uk.gov.nationalarchives.oci.Alphabet.Alphabet
import uk.gov.nationalarchives.oci.{Alphabet, BaseCoder, IncludedAlphabet}

object ConsignmentReference {
  val alphabet: Alphabet = Alphabet.loadAlphabet(Right(IncludedAlphabet.GCRb25)) match {
    case Left(error) => throw new Exception(s"Theres been an error loading the encoding alphabet", error)
    case Right(alphabet) => alphabet
  }
  val baseNumber = 25

  def createConsignmentReference(transferStartYear: Int, consignmentSequenceId: Long): String = {
    val alphabetIndices = BaseCoder.encode(consignmentSequenceId, baseNumber)
    val encoded = Alphabet.toString(alphabet, alphabetIndices)

    s"TDR-$transferStartYear-$encoded"
  }
}
