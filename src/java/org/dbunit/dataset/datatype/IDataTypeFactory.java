/*
 *
 * The DbUnit Database Testing Framework
 * Copyright (C)2002, Manuel Laflamme
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
package org.dbunit.dataset.datatype;

/**
 * A factory for creating {@link DataType}.
 *
 * @author Manuel Laflamme
 * @since May 17, 2003
 * @version $Revision$
 */
public interface IDataTypeFactory
{
    /**
     * Returns the DataType object that corresponds to the specified
     * {@link java.sql.Types}.
     *
     * @param sqlType SQL type from {@link java.sql.Types}
     * @param sqlTypeName Data source dependent type name
     */
    public DataType createDataType(int sqlType, String sqlTypeName) throws DataTypeException;
}
