/*
 * Copyright (c) 2009-2010, Peter Abeles. All Rights Reserved.
 *
 * This file is part of Efficient Java Matrix Library (EJML).
 *
 * EJML is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * EJML is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with EJML.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.ejml.alg.dense.misc;

import org.ejml.alg.generic.CodeGeneratorMisc;

import java.io.FileNotFoundException;
import java.io.PrintStream;


/**
 * Generates unrolled matrix from minor analytical functions.  these can run much faster than LU but will only
 * work for small matrices.
 *
 * When computing the determinants for each minor there are some repeat calculations going on.  I manually
 * removed those by storing them in a local variable and only computing it once.  Despite reducing the FLOP count
 * it didn't seem to noticeably improve performance in a runtime benchmark..
 *
 * @author Peter Abeles
 */
public class GenerateInverseFromMinor {

    String className = "UnrolledInverseFromMinor";
    PrintStream stream;
    int N;

    public GenerateInverseFromMinor() throws FileNotFoundException {
        stream = new PrintStream(className +".java");
    }

    public void createClass(int N) {
        printTop(N);

        printCalls(N);

        for( int i = 2; i <= N; i++ ) {
            printFunction(i);
        }

        stream.print("}\n");
    }

    private void printTop(int N) {
        String foo = CodeGeneratorMisc.COPYRIGHT +
                "\n" +
                "package org.ejml.alg.dense.misc;\n" +
                "\n" +
                "import org.ejml.data.DenseMatrix64F;\n" +
                "\n" +
                "\n" +
                "/**\n" +
                " * This code was auto generated by  {@link GenerateInverseFromMinor} and should not be modified\n" +
                " * directly.  The input matrix is scaled make it much less prone to overflow and underflow issues.\n" +
                " * \n" +
                " * @author Peter Abeles\n" +
                " */\n" +
                "public class "+className+" {\n"+
                "    \n" +
                "    public static final int MAX = "+N+";\n";

        stream.print(foo);
    }

    private void printCalls( int N )
    {
        stream.print(
                "    \n" +
                        "    public static void inv( DenseMatrix64F mat , DenseMatrix64F inv ) {\n");
        stream.print("        double max = Math.abs(mat.data[0]);\n" +
                "        int N = mat.getNumElements();\n" +
                "        \n" +
                "        for( int i = 1; i < N; i++ ) {\n" +
                "            double a = Math.abs(mat.data[i]);\n" +
                "            if( a > max ) max = a;\n" +
                "        }\n\n");
        stream.print(
                "        if( mat.numRows == 2 ) {\n" +
                        "            inv2(mat,inv,1.0/max);\n");
        for( int i = 3; i <= N; i++ ) {
            stream.print("        } else if( mat.numRows == "+i+" ) {\n" +
                    "            inv"+i+"(mat,inv,1.0/max);            \n");
        }
        stream.print("        } else {\n" +
                "            throw new IllegalArgumentException(\"Not supported\");\n" +
                "        }\n" +
                "    }\n\n");
    }

    private void printFunction( int N )
    {
        stream.print("    public static void inv"+N+"( DenseMatrix64F mat , DenseMatrix64F inv , double scale )\n" +
                "    {\n" +
                "        double []data = mat.data;\n"+
                "\n");


        // extracts the first minor
        this.N = N;
        int matrix[] = new int[N*N];
        int index = 0;
        for( int i = 1; i <= N; i++ ) {
            for( int j = 1; j <= N; j++ , index++) {
                matrix[index] = index;
                stream.print("        double "+a(index)+" = data[ "+index+" ]*scale;\n");
            }
        }
        stream.println();

        // compute all the minors
        index = 0;
        for( int i = 1; i <= N; i++ ) {
            for( int j = 1; j <= N; j++ , index++) {
                stream.print("        double m"+i+""+j+" = ");
                if( (i+j) % 2 == 1 )
                    stream.print("-( ");
                printTopMinor(matrix,i-1,j-1,N);
                if( (i+j) % 2 == 1 )
                    stream.print(")");
                stream.print(";\n");
            }
        }

        stream.println();
        // compute the determinant
        stream.print("        double det = (a11*m11");
        for( int i = 2; i <= N; i++ ) {
            stream.print(" + "+a(i-1)+"*m"+1+""+i);
        }
        stream.println(")/scale;");
        stream.println();
        stream.print("        data = inv.data;\n");

        index = 0;
        for( int i = 1; i <= N; i++ ) {
            for( int j = 1; j <= N; j++ , index++) {
                stream.print("        data["+index+"] = m"+j+""+i+" / det;\n");
            }
        }
        stream.println();
        stream.print("    }\n");
        stream.print("\n");
    }

    private void printTopMinor( int m[] , int row , int col , int N )
    {
        int d[] = createMinor(m,row,col,N);

        det(d,0,N-1);
    }

    private int[] createMinor( int m[] , int row , int col , int N )
    {
        int M = N-1;

        int[] ret = new int[ M*M ];

        int index = 0;
        for( int i = 0; i < N; i++ ) {
            if( i == row ) continue;
            for( int j = 0; j < N; j++ ) {
                if( j == col ) continue;

                ret[index++] = m[i*N+j];
            }
        }

        return ret;
    }
    private void det( int m[] , int row , int N )
    {
        if( N == 1 ) {
            stream.print(a(m[0]));
        } else if( N == 2 ) {
            stream.print(a(m[0])+"*"+a(m[3])+" - "+a(m[1])+"*"+a(m[2]));
        } else {
            int M = N-1;

            for( int i = 0; i < N; i++ ) {
                int d[] = createMinor(m,0,i,N);

                int pow = i;

                if( pow % 2 == 0 )
                    stream.print(" + "+a(m[i])+"*(");
                else
                    stream.print(" - "+a(m[i])+"*(");

                det(d,row+1,M);

                stream.print(")");
            }

        }
    }

    private String a( int index )
    {
        int i = index/N+1;
        int j = index%N+1;

        return "a"+i+""+j;
    }

    public static void main( String args[] ) throws FileNotFoundException {
        GenerateInverseFromMinor gen = new GenerateInverseFromMinor();

        gen.createClass(5);
    }
}