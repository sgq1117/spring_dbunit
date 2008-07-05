/*
 *
 * The DbUnit Database Testing Framework
 * Copyright (C)2002-2004, DbUnit.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 */
package org.dbunit.dataset.xml;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.dbunit.dataset.Column;
import org.dbunit.dataset.DataSetException;
import org.dbunit.dataset.DataSetUtils;
import org.dbunit.dataset.DefaultTableMetaData;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.ITableMetaData;
import org.dbunit.dataset.datatype.DataType;
import org.dbunit.dataset.stream.BufferedConsumer;
import org.dbunit.dataset.stream.DefaultConsumer;
import org.dbunit.dataset.stream.IDataSetConsumer;
import org.dbunit.dataset.stream.IDataSetProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

/**
 * @author Manuel Laflamme
 * @since Apr 18, 2003
 * @version $Revision$
 */
public class FlatXmlProducer extends DefaultHandler implements IDataSetProducer, ContentHandler
{

    /**
     * Logger for this class
     */
    private static final Logger logger = LoggerFactory.getLogger(FlatXmlProducer.class);

    private static final IDataSetConsumer EMPTY_CONSUMER = new DefaultConsumer();
    private static final String DATASET = "dataset";

    private final InputSource _inputSource;
    private final EntityResolver _resolver;
    private final boolean _dtdMetadata;
    private boolean _validating = false;
    private IDataSet _metaDataSet;
    
    private int _lineNumber = 0;
    private boolean _dtdPresent = false;
    private boolean _columnSensing = false;

    private IDataSetConsumer _consumer = EMPTY_CONSUMER;
    private ITableMetaData _activeMetaData;
    
    private Map _columnCache = null;

    public FlatXmlProducer(InputSource xmlSource)
    {
        _inputSource = xmlSource;
        _resolver = this;
        _dtdMetadata = true;
    }

    public FlatXmlProducer(InputSource xmlSource, boolean dtdMetadata)
    {
        _inputSource = xmlSource;
        _resolver = this;
        _dtdMetadata = dtdMetadata;
    }

    public FlatXmlProducer(InputSource xmlSource, IDataSet metaDataSet)
    {
        _inputSource = xmlSource;
        _metaDataSet = metaDataSet;
        _resolver = this;
        _dtdMetadata = false;
    }

    public FlatXmlProducer(InputSource xmlSource, EntityResolver resolver)
    {
        _inputSource = xmlSource;
        _resolver = resolver;
        _dtdMetadata = true;
    }
    
    public FlatXmlProducer(InputSource xmlSource, boolean dtdMetadata, boolean columnSensing)
    {
        _inputSource = xmlSource;
        _resolver = this;
        _dtdMetadata = dtdMetadata;
        _columnSensing = columnSensing;
    }
    

    private ITableMetaData createTableMetaData(String tableName, Attributes attributes) throws DataSetException
    {
    	if (logger.isDebugEnabled())
    		logger.debug("createTableMetaData(tableName={}, attributes={}) - start", tableName, attributes);

        if (_metaDataSet != null)
        {
            return _metaDataSet.getTableMetaData(tableName);
        }

        // Create metadata from attributes
        Column[] columns = new Column[attributes.getLength()];
        for (int i = 0; i < attributes.getLength(); i++)
        {
            columns[i] = new Column(attributes.getQName(i), DataType.UNKNOWN);
        }

        return new DefaultTableMetaData(tableName, columns);
    }
    
    
    /**
     * merges the existing columns with the potentially new ones.
     * @param tableName
     * @param attributes
     * @return
     * @throws DataSetException
     */
    private ITableMetaData mergeTableMetaData(String tableName, Attributes attributes) throws DataSetException
    {
    	ITableMetaData tableMetaData = createTableMetaData(tableName, attributes);
        Column[] columns = new Column[attributes.getLength()];
    	if (tableMetaData != null)
    	{
    		// check include all formerly found attributes
    		// might be that there is one attribute missing in this row that was present in one of the former rows
    		columns = DataSetUtils.mergeColumnsByName(_activeMetaData.getColumns(), tableMetaData.getColumns());
    	}
    	
    	return new DefaultTableMetaData(tableName, columns);
    }

    public void setValidating(boolean validating)
    {
        _validating = validating;
    }

    ////////////////////////////////////////////////////////////////////////////
    // IDataSetProducer interface

    public void setConsumer(IDataSetConsumer consumer) throws DataSetException
    {
        logger.debug("setConsumer(consumer) - start");

        if(this._columnSensing) {
            _consumer = new BufferedConsumer(consumer);
        }
        else {
            _consumer = consumer;
        }
    }

    public void produce() throws DataSetException
    {
        logger.debug("produce() - start");

        try
        {
            SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
            saxParserFactory.setValidating(_validating);
            XMLReader xmlReader = saxParserFactory.newSAXParser().getXMLReader();

            if (_dtdMetadata)
            {
                FlatDtdHandler dtdHandler = new FlatDtdHandler();
                FlatDtdHandler.setLexicalHandler(xmlReader, dtdHandler);
                FlatDtdHandler.setDeclHandler(xmlReader, dtdHandler);
            }

            xmlReader.setContentHandler(this);
            xmlReader.setErrorHandler(this);
            xmlReader.setEntityResolver(_resolver);
            xmlReader.parse(_inputSource);
        }
        catch (ParserConfigurationException e)
        {
            throw new DataSetException(e);
        }
        catch (SAXException e)
        {
            int lineNumber = -1;
            if (e instanceof SAXParseException)
            {
                lineNumber = ((SAXParseException)e).getLineNumber();
            }
            Exception exception = e.getException() == null ? e : e.getException();

            if (lineNumber >= 0)
            {
                String message = "Line " + lineNumber + ": " + exception.getMessage();
                throw new DataSetException(message, e);
            }
            throw new DataSetException(exception);
        }
        catch (IOException e)
        {
            throw new DataSetException(e);
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    // EntityResolver interface

    public InputSource resolveEntity(String publicId, String systemId)
            throws SAXException
    {
        logger.debug("resolveEntity(publicId={}, systemId={}) - start", publicId, systemId);

        if (!_dtdMetadata)
        {
            return new InputSource(new StringReader(""));
        }
        return null;
    }

    ////////////////////////////////////////////////////////////////////////////
    // ErrorHandler interface

    public void error(SAXParseException e) throws SAXException
    {
        throw e;

    }

    ////////////////////////////////////////////////////////////////////////
    // ContentHandler interface

    public void startElement(String uri, String localName, String qName,
            Attributes attributes) throws SAXException
    {
    	if (logger.isDebugEnabled())
    		logger.debug("startElement(uri={}, localName={}, qName={}, attributes={}) - start",
    				new Object[] { uri, localName, qName, attributes });

        try
        {
            // Start of dataset
            if (_activeMetaData == null && qName.equals(DATASET))
            {
                _consumer.startDataSet();
                return;
            }

            // New table
            if (_activeMetaData == null || !_activeMetaData.getTableName().equals(qName))
            {
                // If not first table, notify end of previous table to consumer
                if (_activeMetaData != null)
                {
                    _consumer.endTable();
                }

                // Notify start of new table to consumer
                _activeMetaData = createTableMetaData(qName, attributes);
                
                _columnCache = new HashMap();
                for (int i = 0; i < _activeMetaData.getColumns().length ; i++) {
                	Column column = _activeMetaData.getColumns()[i];
                	_columnCache.put(column.getColumnName(), column);
                }
                
                _consumer.startTable(_activeMetaData);
                _lineNumber = 0;
            }

            // Row notification
            if (attributes.getLength() > 0)
            {
            	for (int i = 0 ; i < attributes.getLength(); i++)
            	{
            		if (_columnCache.get(attributes.getQName(i)) == null) 
            		{
                    	if (_columnSensing)
                    	{
                    		logger.debug("Column sensing enabled. Will create a new metaData with potentially new columns if needed");
                    		_activeMetaData = mergeTableMetaData(qName, attributes);
                    		// We also need to recreate the table, copying the data already collected from the old one to the new one
                    		_consumer.startTable(_activeMetaData);
                    	} 
                    	else
                    	{
	                		logger.warn("Extra columns on line " + (_lineNumber+1) 
	                				+ ".  Those columns will be ignored.");
	                		logger.warn("Please add the extra columns to line 1,"
	                				+ " or use a DTD to make sure the value of those columns are populated " +
	                						"or specify 'columnSensing=true' for your FlatXmlProducer.");
	                		logger.warn("See FAQ for more details.");
                    	}

            		}
            	}
            	
            	_lineNumber++;
                Column[] columns = _activeMetaData.getColumns();
                Object[] rowValues = new Object[columns.length];
                for (int i = 0; i < columns.length; i++)
                {
                    Column column = columns[i];
                    rowValues[i] = attributes.getValue(column.getColumnName());
                }
                _consumer.row(rowValues);
            }
        }
        catch (DataSetException e)
        {
            throw new SAXException(e);
        }
    }

    public void endElement(String uri, String localName, String qName) throws SAXException
    {
    	if (logger.isDebugEnabled())
    		logger.debug("endElement(uri={}, localName={}, qName={}) - start",
    				new Object[]{ uri, localName, qName });

        // End of dataset
        if (qName.equals(DATASET))
        {
            try
            {
                // Notify end of active table to consumer
                if (_activeMetaData != null)
                {
                    _consumer.endTable();
                }

                // Notify end of dataset to consumer
                _consumer.endDataSet();
            }
            catch (DataSetException e)
            {
                throw new SAXException(e);
            }
        }
    }

    private class FlatDtdHandler extends FlatDtdProducer
    {

        /**
         * Logger for this class
         */
        private final Logger logger = LoggerFactory.getLogger(FlatDtdHandler.class);

        public FlatDtdHandler()
        {
        }

        ////////////////////////////////////////////////////////////////////////////
        // LexicalHandler interface

        public void startDTD(String name, String publicId, String systemId)
                throws SAXException
        {
        	if (logger.isDebugEnabled())
        		logger.debug("startDTD(name={}, publicId={}, systemId={}) - start",
        				new Object[] { name, publicId, systemId });

            _dtdPresent = true;
            try
            {
                // Cache the DTD content to use it as metadata
                FlatDtdDataSet metaDataSet = new FlatDtdDataSet();
                this.setConsumer(metaDataSet);
                _metaDataSet = metaDataSet;

                super.startDTD(name, publicId, systemId);
            }
            catch (DataSetException e)
            {
                throw new SAXException(e);
            }
        }
    }

}
