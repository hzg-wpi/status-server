/*
 * The main contributor to this project is Institute of Materials Research,
 * Helmholtz-Zentrum Geesthacht,
 * Germany.
 *
 * This project is a contribution of the Helmholtz Association Centres and
 * Technische Universitaet Muenchen to the ESS Design Update Phase.
 *
 * The project's funding reference is FKZ05E11CG1.
 *
 * Copyright (c) 2012. Institute of Materials Research,
 * Helmholtz-Zentrum Geesthacht,
 * Germany.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA
 */

package wpn.hdri.ss.data;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.onjava.lang.DoubleToString;

import java.lang.reflect.Array;
import java.util.Map;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 06.08.12
 */
public class ValueFormatters {
    private ValueFormatters() {
    }

    /**
     * Neither parameter can be null.
     *
     * @param clazz     an encapsulated in value class (Double.class, String.class etc)
     * @param formatter {@link ValueFormatter} implementation
     * @param <T>
     * @throws NullPointerException if any of the parameters is null
     */
    public static <T> void registerFormatter(Class<T> clazz, ValueFormatter<T> formatter) {
        Preconditions.checkNotNull(clazz);
        Preconditions.checkNotNull(formatter);
        FORMATTERS.put(clazz, formatter);
    }

    public static <T> ValueFormatter<T> getFormatter(Class<T> clazz) {
        if(clazz.isArray()){
            return(ValueFormatter<T>) ARRAY_FORMATTER;
        }
        ValueFormatter<?> formatter = FORMATTERS.get(clazz);
        if (formatter != null)
            return (ValueFormatter<T>) formatter;
        else
            return (ValueFormatter<T>) DEFAULT_FORMATTER;
    }

    public static <T> ValueFormatter<T> getDefaultFormatter() {
        return (ValueFormatter<T>) DEFAULT_FORMATTER;
    }

    public static <T> ValueFormatter<T> getNullFormatter() {
        return (ValueFormatter<T>) NULL_FORMATTER;
    }

    private static final int MAX_PRECISION = 128;
    private static final long POW10[] = new long[MAX_PRECISION + 1];
    private static final double NEGATIVE_POW10[] = new double[MAX_PRECISION + 1];
    static{
        for(int i = 0; i <= MAX_PRECISION; ++i){
            POW10[i] = (long)Math.pow(10,i);
            NEGATIVE_POW10[i] = Math.pow(10,-i);
        }
    }

    public static final ValueFormatter<Double> DOUBLE_FORMATTER = new ValueFormatter<Double>() {
        private ThreadLocal<DoubleToString> tlDoubleToString = new ThreadLocal<DoubleToString>(){
            @Override
            protected DoubleToString initialValue() {
                return new DoubleToString();
            }
        };

        private ThreadLocal<StringBuffer> tlBld = new ThreadLocal<StringBuffer>(){
            @Override
            protected StringBuffer initialValue() {
                return new StringBuffer();
            }
        };

        @Override
        public String format(Double value) {
            double val = value;

            DoubleToString doubleToString = tlDoubleToString.get();
            StringBuffer bld = tlBld.get();
            doubleToString.append(bld,val);

            try {
                return bld.toString();
            } finally {
                bld.setLength(0);
            }
        }
    };

    public static final ValueFormatter<?> DEFAULT_FORMATTER = new ValueFormatter<Object>() {
        @Override
        public String format(Object value) {
            return String.valueOf(value);
        }
    };

    public static final ValueFormatter<?> ARRAY_FORMATTER = new ValueFormatter<Object>() {
        @Override
        public String format(Object value) {
            StringBuilder result = new StringBuilder();
            int length = Array.getLength(value);
            for ( int idx = 0 ; idx < length ; ++idx ) {
                Object item = Array.get(value, idx);
                ValueFormatter<Object> formatter = ValueFormatters.<Object>getFormatter((Class<Object>)item.getClass());
                result.append( formatter.format(item) );
                if ( idx !=  length - 1) {
                    result.append(';');
                }
            }
            return result.toString();
        }
    };

    public static final ValueFormatter<?> NULL_FORMATTER = new ValueFormatter<Object>() {
        @Override
        public String format(Object value) {
            return "NA";
        }
    };

    private static final Map<Class<?>, ValueFormatter<?>> FORMATTERS = Maps.newIdentityHashMap();

    static {
        FORMATTERS.put(Double.class, ValueFormatters.DOUBLE_FORMATTER);
        FORMATTERS.put(double.class, ValueFormatters.DOUBLE_FORMATTER);
    }
}
