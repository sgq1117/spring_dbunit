/*
 *
 * DbUnit Database Testing Framework
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

import java.sql.Types;
import java.util.Arrays;

/**
 * @author Manuel Laflamme
 * @version $Revision$
 */

public class BytesDataTypeTest extends AbstractDataTypeTest
{
    private final static DataType[] TYPES = {
        DataType.BINARY,
        DataType.VARBINARY,
        DataType.LONGVARBINARY,
        DataType.BLOB,
    };

    public BytesDataTypeTest(String name)
    {
        super(name);
    }

    public void testToString() throws Exception
    {
        String[] expected = {"BINARY", "VARBINARY", "LONGVARBINARY", "BLOB"};

        assertEquals("type count", expected.length, TYPES.length);
        for (int i = 0; i < TYPES.length; i++)
        {
            assertEquals("name", expected[i], TYPES[i].toString());
        }
    }

    public void testGetTypeClass() throws Exception
    {
        for (int i = 0; i < TYPES.length; i++)
        {
            assertEquals("class", byte[].class, TYPES[i].getTypeClass());
        }
    }

    public void testIsNumber() throws Exception
    {
        for (int i = 0; i < TYPES.length; i++)
        {
            assertEquals("is number", false, TYPES[i].isNumber());
        }
    }

    public void testTypeCast() throws Exception
    {
        Object[] values = {
            null,
            "",
            "YWJjZA==",
            new byte[]{0, 1, 2, 3, 4, 5},
        };

        byte[][] expected = {
            null,
            new byte[0],
            new byte[]{'a', 'b', 'c', 'd'},
            new byte[]{0, 1, 2, 3, 4, 5},
        };

        assertEquals("actual vs expected count", values.length, expected.length);

        for (int i = 0; i < TYPES.length; i++)
        {
            for (int j = 0; j < values.length; j++)
            {
                byte[] actual = (byte[])TYPES[i].typeCast(values[j]);
                assertTrue("typecast " + j, Arrays.equals(expected[j], actual));
            }
        }
    }

    public void testTypeCastInvalid() throws Exception
    {
        Object[] values = {
            new Object(),
            new Integer(1234)
        };

        for (int i = 0; i < TYPES.length; i++)
        {
            for (int j = 0; j < values.length; j++)
            {
                try
                {
                    TYPES[i].typeCast(values[j]);
                    fail("Should throw TypeCastException");
                }
                catch (TypeCastException e)
                {
                }
            }
        }
    }

    public void testCompareEquals() throws Exception
    {
        Object[] values1 = {
            null,
            "",
            "YWJjZA==",
            new byte[]{0, 1, 2, 3, 4, 5},
        };

        byte[][] values2 = {
            null,
            new byte[0],
            new byte[]{'a', 'b', 'c', 'd'},
            new byte[]{0, 1, 2, 3, 4, 5},
        };

        assertEquals("values count", values1.length, values2.length);

        for (int i = 0; i < TYPES.length; i++)
        {
            for (int j = 0; j < values1.length; j++)
            {
                assertEquals("compare1 " + j, 0, TYPES[i].compare(values1[j], values2[j]));
                assertEquals("compare2 " + j, 0, TYPES[i].compare(values2[j], values1[j]));
            }
        }
    }

    public void testCompareInvalid() throws Exception
    {
        Object[] values1 = {
            new Object(),
            new java.util.Date()
        };
        Object[] values2 = {
            null,
            null
        };

        assertEquals("values count", values1.length, values2.length);

        for (int i = 0; i < TYPES.length; i++)
        {
            for (int j = 0; j < values1.length; j++)
            {
                try
                {
                    TYPES[i].compare(values1[j], values2[j]);
                    fail("Should throw TypeCastException");
                }
                catch (TypeCastException e)
                {
                }

                try
                {
                    TYPES[i].compare(values2[j], values1[j]);
                    fail("Should throw TypeCastException");
                }
                catch (TypeCastException e)
                {
                }
            }
        }
    }

    public void testCompareDifferent() throws Exception
    {
        Object[] less = {
            null,
            new byte[]{'a', 'a', 'c', 'd'},
            new byte[]{0, 1, 2, 3, 4, 5},
        };
        Object[] greater = {
            new byte[0],
            new byte[]{'a', 'b', 'c', 'd'},
            new byte[]{0, 1, 2, 3, 4, 5, 6},
        };

        assertEquals("values count", less.length, greater.length);

        for (int i = 0; i < TYPES.length; i++)
        {
            for (int j = 0; j < less.length; j++)
            {
                assertTrue("less " + j, TYPES[i].compare(less[j], greater[j]) < 0);
                assertTrue("greater " + j, TYPES[i].compare(greater[j], less[j]) > 0);
            }
        }
    }

    public void testSqlType() throws Exception
    {
        int[] sqlTypes = {Types.BINARY, Types.VARBINARY, Types.LONGVARBINARY, Types.BLOB};

        assertEquals("count", sqlTypes.length, TYPES.length);
        for (int i = 0; i < TYPES.length; i++)
        {
            assertEquals("forSqlType", TYPES[i], DataType.forSqlType(sqlTypes[i]));
            assertEquals("forSqlTypeName", TYPES[i], DataType.forSqlTypeName(TYPES[i].toString()));
            assertEquals("getSqlType", sqlTypes[i], TYPES[i].getSqlType());
        }
    }

    public void testForObject() throws Exception
    {
        assertEquals(DataType.VARBINARY, DataType.forObject(new byte[0]));
    }

    public void testAsString() throws Exception
    {
        byte[][] values = {
            new byte[0],
            new byte[]{'a', 'b', 'c', 'd'},
        };

        String[] expected = {
            "",
            "YWJjZA==",
        };

        assertEquals("actual vs expected count", values.length, expected.length);

        for (int i = 0; i < values.length; i++)
        {
            assertEquals("asString " + i, expected[i], DataType.asString(values[i]));
        }
    }
}





