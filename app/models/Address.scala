package models

import scala.collection.mutable.ListBuffer

import com.google.maps.{GeoApiContext, GeocodingApi}
import com.google.maps.model.GeocodingResult

case class Address(address: String, lat: Double, lng: Double) {
  override def toString = address
}

object Address {
  private val _context = new GeoApiContext.Builder().apiKey(sys.env("MAPS_API_KEY")).build();

  def geocode(address: String) = {
    val results = GeocodingApi.geocode(_context, address).await()
    var addresses = new ListBuffer[Address]()
    for (result <- results) {
      addresses += Address(result.formattedAddress, result.geometry.location.lat, result.geometry.location.lng)
    }
    addresses.to[List]
  }
}
