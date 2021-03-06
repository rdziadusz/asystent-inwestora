package com.agsupport.core.service.collection;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Schedule;
import javax.ejb.Stateless;
import javax.ejb.Timeout;

import org.jboss.logging.Logger;

import com.agsupport.core.jpa.facade.DerivativeFacade;
import com.agsupport.core.jpa.facade.DerivativeValueFacade;
import com.agsupport.core.jpa.facade.DerivativeValueFacade;
import com.agsupport.core.jpa.facade.StockMartekFacade;
import com.agsupport.core.jpa.model.Derivative;
import com.agsupport.core.jpa.model.DerivativeValue;
import com.agsupport.core.jpa.model.StockMarket;
import com.agsupport.parser.derivative.DerivativeParser;

/**
 * Klasa odpowiedzialna za systematyczne pobieranie wartosci instrumentów
 * pochodnych
 * 
 * @author Michał Gruszczyński
 * 
 */

@Stateless
public class DerivativeValueCollector {

	private Logger logger = Logger.getLogger(DerivativeValue.class);

	@EJB
	private DerivativeFacade derivativeFacade;
	@EJB
	private DerivativeValueFacade derivativeValueFacade;

	@PostConstruct
	public void init() {
		logger.info("DerivativeValueCollector.init START");
	}

	/**
	 * Metoda Timer Service. Wywoływana co 30 minut. Sekwencyjnie wywołuje
	 * parsery różnych stron z których pobierane są dane na temat instrumentow
	 * pochodnych
	 * 
	 */
	@Schedule(persistent = false, second = "0", minute = "*/30", hour = "*")
	public void collect() {
		logger.info("DerivativeValueCollector.collect START");
		List<DerivativeParser> parserList = new LinkedList<DerivativeParser>();
		parserList.add(new DerivativeParser());
		parserList.add(new DerivativeParser());

		for (DerivativeParser p : parserList) {
			/*
			 * data dodania zbioru wartości indeksów do bazy dla danego parsera
			 * data jest również wykorzystana gdyby okazało się że derivative
			 * nie istnieje jeszcze w bazie
			 */
			Date dateOfAdd = new Date();

			Map<String, DerivativeValue> map = p.getDerivativeValueList();
			for (Map.Entry<String, DerivativeValue> e : map.entrySet()) {
				String derivativeName = e.getKey();
				DerivativeValue derivativeValue = e.getValue();
				derivativeValue.setDateOfAdd(dateOfAdd);
				addDerivativeValue(derivativeName, derivativeValue);
			}
		}
		logger.info("DerivativeValueCollector.collect END");
	}

	/**
	 * Dodanie wartości instrumentu pochodnego
	 * 
	 * @param nameOfStockMarket
	 *            nazwa instrumentu pochodnego
	 * @param stockIndex
	 *            wartość instrumentu pochodnego
	 * @return
	 */
	private boolean addDerivativeValue(String derivativeName,
			DerivativeValue derivativeValue) {
		Derivative derivative = null;

		derivative = derivativeFacade.getDerivativeByName(derivativeName);

		if (derivative == null) {
			// próba dodatnia instrumentu pochodnego do bazy

			derivative = createDerivative(derivativeName);

			if (derivative == null) {
				// Giełda nadal nie istnieje - brak integralności bazy.
				logger.info("DerivativeValueCollector.addDerivativeValue - derivative = null / NOT CREATED");
				logger.info("DerivativeValueCollector.addDerivativeValue NOT DONE!");
				return false;
			}
		}

		logger.info("DerivativeValueCollector.addDerivativeValue ");

		if (derivativeFacade.addDerivativeValue(derivative.getId(),
				derivativeValue) == true) {
			logger.info("Dodano DerivativeValue do instrumentu pochodnego o id = "
					+ derivative.getId());
			return true;
		} else {
			logger.info("NIE dodano DerivativeValue do instrumentu pochodnego o id = "
					+ derivative.getId());
			logger.info("Możliwy powód - zduplikowanie wartości!");
			return false;
		}
	}

	/**
	 * Dodanie nowego instrumentu pochodnego do bazy danych i natychmiastowe
	 * pobranie go
	 * 
	 * @param nameOfDerivative
	 *            nazwa instrumentu pochodnego
	 * @return
	 */
	private Derivative createDerivative(String nameOfDerivative) {
		// Gdy nie istnieje dany instrument pochodny - zostaje autmatycznie
		// dodana do bazy
		logger.info("DerivativeValueCollector.addDerivativeValue - stockMarket = null");
		Derivative derivative = new Derivative();
		derivative.setName(nameOfDerivative);
		derivative.setDateOfAdd(new Date());
		derivativeFacade.createDerivative(derivative);
		derivative = derivativeFacade.getDerivativeByName(nameOfDerivative);
		return derivative;
	}

	@Timeout
	public void timeout() {
		logger.info("Invoke.timeout STOCKindexCOLLECTOR");
	}

	public Logger getLogger() {
		return logger;
	}

	public void setLogger(Logger logger) {
		this.logger = logger;
	}

	public DerivativeFacade getDerivativeFacade() {
		return derivativeFacade;
	}

	public void setDerivativeFacade(DerivativeFacade derivativeFacade) {
		this.derivativeFacade = derivativeFacade;
	}

	public DerivativeValueFacade getDerivativeValueFacade() {
		return derivativeValueFacade;
	}

	public void setDerivativeValueFacade(
			DerivativeValueFacade derivativeValueFacade) {
		this.derivativeValueFacade = derivativeValueFacade;
	}

}
