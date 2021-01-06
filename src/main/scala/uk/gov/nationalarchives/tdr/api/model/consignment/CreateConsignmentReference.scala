package uk.gov.nationalarchives.tdr.api.model.consignment

import uk.gov.nationalarchives.oci.{Alphabet, BaseCoder, IncludedAlphabet}

class CreateConsignmentReference {
  def createConsignmentReference(createdDate: Int, consignmentSequenceId: Long): String = {
    val baseNumber = 25
    val encoded = Alphabet.loadAlphabet(Right(IncludedAlphabet.GCRb25)) match {
      case Left(error) => throw new Exception(s"Theres been an error creating the consignment reference: ${error.getMessage}")
      case Right(alphabet) =>
        val alphabetIndices = BaseCoder.encode(consignmentSequenceId, baseNumber)
        Alphabet.toString(alphabet, alphabetIndices)
    }
    s"TDR-$createdDate-$encoded"
  }
}
