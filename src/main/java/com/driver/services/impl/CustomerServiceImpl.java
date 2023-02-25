package com.driver.services.impl;

import com.driver.model.*;
import com.driver.services.CustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.driver.repository.CustomerRepository;
import com.driver.repository.DriverRepository;
import com.driver.repository.TripBookingRepository;

import java.util.List;

@Service
public class CustomerServiceImpl implements CustomerService {

	@Autowired
	CustomerRepository customerRepository2;

	@Autowired
	DriverRepository driverRepository2;

	@Autowired
	TripBookingRepository tripBookingRepository2;

	@Override
	public void register(Customer customer) {
		//Save the customer in database
		customerRepository2.save(customer);
	}

	@Override
	public void deleteCustomer(Integer customerId) {
		// Delete customer without using deleteById function
		Customer customer = customerRepository2.findById(customerId).get();
		List<TripBooking> bookedTrips = customer.getTripBookingList();

		//Now we will set the cab as available for each and every trip booked by this customer,
		//who is going to be deleted

		for(TripBooking trip : bookedTrips){
			Driver driver = trip.getDriver();
			Cab cab = driver.getCab();
			cab.setAvailable(true);
			driverRepository2.save(driver);
		}

		/* We are doing all these above things because customer table is not joined with the driver or cab table
		* directly, hence cascading will not work for the driver here, therefore, we are making the changes
		* by manually, and since driver is parent and cab is child making changes in parent (driver) will
		* automatically make changes in child (cab)*/

		//Now we will delete the customer from the repository and as a result of cascading effect trips will also
		//be deleted
		customerRepository2.delete(customer);
	}

	@Override
	public TripBooking bookTrip(int customerId, String fromLocation, String toLocation, int distanceInKm) throws Exception{
		//Book the driver with lowest driverId who is free (cab available variable is Boolean.TRUE). If no driver is available, throw "No cab available!" exception
		//Avoid using SQL query

		List<Driver> driverList = driverRepository2.findAll(Sort.by(Sort.Direction.ASC, "driverId"));
		Driver driver = null;
		for(Driver currDriver : driverList){
			if(currDriver.getCab().getAvailable()){
				driver = currDriver;
				break;
			}
		}
		if(driver==null) {
			throw new Exception("No cab available!");
		}

		TripBooking newTripBooked = new TripBooking();
		newTripBooked.setCustomer(customerRepository2.findById(customerId).get());
		newTripBooked.setFromLocation(fromLocation);
		newTripBooked.setToLocation(toLocation);
		newTripBooked.setDistanceInKm(distanceInKm);
		newTripBooked.setStatus(TripStatus.CONFIRMED);
		newTripBooked.setDriver(driver);
		int rate = driver.getCab().getPerKmRate();
		newTripBooked.setBill(distanceInKm*rate);

		driver.getCab().setAvailable(false);
		driverRepository2.save(driver);

		Customer customer = customerRepository2.findById(customerId).get();
		customer.getTripBookingList().add(newTripBooked);
		customerRepository2.save(customer);


		tripBookingRepository2.save(newTripBooked);
		return newTripBooked;
	}

	@Override
	public void cancelTrip(Integer tripId){
		//Cancel the trip having given trip Id and update TripBooking attributes accordingly
		TripBooking bookedTrip = tripBookingRepository2.findById(tripId).get();
		bookedTrip.setStatus(TripStatus.CANCELED);
		bookedTrip.setBill(0);
		bookedTrip.getDriver().getCab().setAvailable(true);
		tripBookingRepository2.save(bookedTrip);
	}

	@Override
	public void completeTrip(Integer tripId){
		//Complete the trip having given trip Id and update TripBooking attributes accordingly
		TripBooking bookedTrip = tripBookingRepository2.findById(tripId).get();
		bookedTrip.setStatus(TripStatus.COMPLETED);
		bookedTrip.getDriver().getCab().setAvailable(true);
		tripBookingRepository2.save(bookedTrip);
	}
}
