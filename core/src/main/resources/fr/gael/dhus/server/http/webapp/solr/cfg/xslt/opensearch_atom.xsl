<?xml version="1.0" encoding="UTF-8"?>
<!--
   Data Hub Service (DHuS) - For Space data distribution.
   Copyright (C) 2013-2019 GAEL Systems

   This file is part of DHuS software sources.

   This program is free software: you can redistribute it and/or modify
   it under the terms of the GNU Affero General Public License as
   published by the Free Software Foundation, either version 3 of the
   License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
   GNU Affero General Public License for more details.

   You should have received a copy of the GNU Affero General Public License
   along with this program. If not, see <http://www.gnu.org/licenses/>.
-->

<!--
    Sample OpenSearch Atom search results representation.
-->
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
		xmlns:java="http://xml.apache.org/xslt/java"
      exclude-result-prefixes="java">
    <xsl:output method="xml" encoding="utf-8" indent="yes" media-type="application/atom+xml;charset=UTF-8" />
    <!-- This template matches root element not having a <result> child (error) -->
    <xsl:template match="/response[not(result)]">
       <xsl:variable name="errorMessage" select="lst[@name='error']/str[@name='msg']" />
       <xsl:variable name="errorCode"    select="lst[@name='error']/int[@name='code']" />
       <feed xmlns="http://www.w3.org/2005/Atom" xmlns:opensearch="http://a9.com/-/spec/opensearch/1.1/">
          <error>
            <code><xsl:value-of select="$errorCode" /></code>
            <message><xsl:value-of select="$errorMessage" /></message>
          </error>
       </feed>
    </xsl:template>

    <!-- This template matches root element having a <result> child -->
    <xsl:template match="/response[result]">
        <xsl:variable name="searchTerms" select="lst[@name='responseHeader']/lst[@name='params']/str[@name='q']" />
        <xsl:variable name="totalHits" select="result/@numFound" />
        <xsl:variable name="searchTimeSecs" select="number(lst[@name='responseHeader']/int[@name='QTime']) div 1000.0" />
        <xsl:variable name="dhusLongname" select="lst[@name='responseHeader']/lst[@name='params']/str[@name='dhusLongName']" />
        <xsl:variable name="originalQuery" select="lst[@name='responseHeader']/lst[@name='params']/str[@name='originalQuery']" />
        <xsl:variable name="dhusServer" select="lst[@name='responseHeader']/lst[@name='params']/str[@name='dhusServer']" />
        <xsl:variable name="format">
            <xsl:if test="lst[@name='responseHeader']/lst[@name='params']/str[@name='format']">
                <xsl:value-of select="concat('&amp;format=', lst[@name='responseHeader']/lst[@name='params']/str[@name='format'])" />
            </xsl:if>
        </xsl:variable>
        <xsl:variable name="type" select="lst[@name='responseHeader']/lst[@name='params']/str[@name='type']" />
        <xsl:variable name="start">
            <xsl:choose>
                <xsl:when test="lst[@name='responseHeader']/lst[@name='params']/str[@name='start'] != ''">
                    <xsl:value-of select="lst[@name='responseHeader']/lst[@name='params']/str[@name='start']" />
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="0" />
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <xsl:variable name="rows">
            <xsl:choose>
                <xsl:when test="lst[@name='responseHeader']/lst[@name='params']/str[@name='rows'] != ''">
                    <xsl:value-of select="lst[@name='responseHeader']/lst[@name='params']/str[@name='rows']" />
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="10" />
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <xsl:variable name="orderby">
            <xsl:if test="lst[@name='responseHeader']/lst[@name='params']/str[@name='orderby']">
                <xsl:value-of select="concat('&amp;orderby=', response/lst[@name='responseHeader']/lst[@name='params']/str[@name='orderby'])" />
            </xsl:if>
        </xsl:variable>
        <xsl:variable name="totalPages" select="ceiling(number($totalHits) div number($rows))" />
        <xsl:variable name="currentPage" select="ceiling(number($start) div number($rows))" />
        <feed xmlns="http://www.w3.org/2005/Atom" xmlns:opensearch="http://a9.com/-/spec/opensearch/1.1/">
            <title><xsl:value-of select="$dhusLongname" /> search results for: <xsl:value-of select="$originalQuery" /></title>
        	<xsl:choose>
                <xsl:when test="number($totalHits) &gt; number($rows) and (number($totalHits) - number($start)) &gt; number($rows)">
                	<subtitle>Displaying <xsl:value-of select="$start" /> to <xsl:value-of select="$start + $rows - 1" /> of <xsl:value-of select="$totalHits" /> total results. Request done in <xsl:value-of select="$searchTimeSecs" /> seconds.</subtitle>
                </xsl:when>
                <xsl:when test="number($totalHits) &gt; number($rows) and number($start) &gt; 0">
                	<subtitle>Displaying <xsl:value-of select="$start" /> to <xsl:value-of select="$totalHits - 1" /> of <xsl:value-of select="$totalHits" /> total results. Request done in <xsl:value-of select="$searchTimeSecs" /> seconds.</subtitle>
                </xsl:when>
                <xsl:otherwise>
                	<subtitle>Displaying <xsl:value-of select="$totalHits" /> results. Request done in <xsl:value-of select="$searchTimeSecs" /> seconds.</subtitle>
                </xsl:otherwise>
            </xsl:choose>
            <updated><xsl:value-of select='java:format(java:java.text.SimpleDateFormat.new("yyyy-MM-dd"), java:java.util.Date.new())' />T<xsl:value-of select='java:format(java:java.text.SimpleDateFormat.new("HH:mm:ss.SSS"), java:java.util.Date.new())' />Z</updated>
            <author><name><xsl:value-of select="$dhusLongname" /></name></author>
            <id><xsl:value-of select="$dhusServer" />search?q=<xsl:value-of select="$originalQuery" /></id>
            <opensearch:totalResults><xsl:value-of select="$totalHits" /></opensearch:totalResults>
            <opensearch:startIndex><xsl:value-of select="$start" /></opensearch:startIndex>
            <opensearch:itemsPerPage><xsl:value-of select="$rows" /></opensearch:itemsPerPage>
            <opensearch:Query role="request" searchTerms="{$originalQuery}" startPage="1" />
            <!-- You might want to add another stylesheet to emit HTML...
            <link rel="alternate" type="text/html"
                href="{$dhusServer}search?q={$originalQuery}&amp;start=TODO&amp;rows=TODO" />
            -->
            <!--
                You should use XPath 2.0 functions to "escape-uri" these link href state transitions.
                See http://wiki.apache.org/solr/XsltResponseWriter for help on switching to XPath 2.0
            -->
            <link rel="self" type="{$type}"
                href="{$dhusServer}search?q={$originalQuery}&amp;start={$start}&amp;rows={$rows}{$orderby}{$format}" />
            <link rel="first" type="{$type}"
                href="{$dhusServer}search?q={$originalQuery}&amp;start=0&amp;rows={$rows}{$orderby}{$format}" />
            <xsl:choose>
                <xsl:when test="number($start) &gt; number($rows)">
                    <xsl:variable name="previous" select="(number($currentPage) - 1) * number($rows)" />
                    <link rel="previous" type="{$type}"
                        href="{$dhusServer}search?q={$originalQuery}&amp;start={$previous}&amp;rows={$rows}{$orderby}{$format}" />
                </xsl:when>
            </xsl:choose>
            <xsl:choose>
                <xsl:when test="number($totalHits) &gt; number($rows) and (number($totalHits) - number($start)) &gt; number($rows)">
                    <xsl:variable name="next" select="(number($currentPage) + 1) * number($rows)" />
                    <link rel="next" type="{$type}"
                        href="{$dhusServer}search?q={$originalQuery}&amp;start={$next}&amp;rows={$rows}{$orderby}{$format}" />
                </xsl:when>
            </xsl:choose>
            <link rel="last" type="{$type}"
                href="{$dhusServer}search?q={$originalQuery}&amp;start={number($totalHits) - 1}&amp;rows={$rows}{$orderby}{$format}" />
            <!-- autodiscovery tag -->
            <link rel="search" type="application/opensearchdescription+xml"
                href="opensearch_description.xml" />
            <xsl:for-each select="result/doc">
                <xsl:variable name="id" select="long[@name='id']" />
                <xsl:variable name="uuid" select="str[@name='uuid']" />
                <entry>
                    <title>
                        <xsl:value-of select="str[@name='identifier']" />
                    </title>
                    <link>
                    	<xsl:attribute name="href"><xsl:value-of select="$dhusServer" />odata/v1/Products('<xsl:value-of select="$uuid" />')/$value</xsl:attribute>
                    </link>
                    <link>
                    	<xsl:attribute name="rel">alternative</xsl:attribute>
                    	<xsl:attribute name="href"><xsl:value-of select="$dhusServer" />odata/v1/Products('<xsl:value-of select="$uuid" />')/</xsl:attribute>
                    </link>
                    <link>
                    	<xsl:attribute name="rel">icon</xsl:attribute>
                    	<xsl:attribute name="href"><xsl:value-of select="$dhusServer" />odata/v1/Products('<xsl:value-of select="$uuid" />')/Products('Quicklook')/$value</xsl:attribute>
                    </link>
                    <id><xsl:value-of select="$uuid" /></id>
                    <summary>
                        <xsl:variable name="contentDate" select="date[@name='beginposition']" />
                        <xsl:variable name="instrument"  select="str[@name='instrumentshortname']" />
                        <xsl:variable name="mode"  select="str[@name='polarisationmode']" />
                        <xsl:variable name="satellite"  select="str[@name='platformname']" />
                        <xsl:variable name="size"  select="str[@name='size']" />
                        <xsl:choose>
                            <xsl:when test="str[@name='polarisationmode']">
                                <xsl:value-of
                                   select="concat('Date: ', $contentDate,
                                      ', Instrument: ', $instrument,
                                      ', Mode: ', $mode,
                                      ', Satellite: ', $satellite,
                                      ', Size: ', $size)"/>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:value-of
                                   select="concat('Date: ', $contentDate,
                                      ', Instrument: ', $instrument,
                                      ', Satellite: ', $satellite,
                                      ', Size: ', $size)"/>
                            </xsl:otherwise>
                        </xsl:choose>
                    </summary>
                    <xsl:choose>
                        <xsl:when test="bool[@name='ondemand']">
                             <ondemand><xsl:value-of select="bool[@name='ondemand']"/></ondemand>
                        </xsl:when>
                        <xsl:otherwise>
                             <ondemand><xsl:value-of select="false()"/></ondemand>
                        </xsl:otherwise>
                    </xsl:choose>
                    <xsl:copy-of select="date" />
                    <xsl:copy-of select="boolean" />
                    <xsl:copy-of select="int[@name != 'id' and @name != '_version_']" />
                    <xsl:copy-of select="double" />
                    <xsl:copy-of select="str[@name != 'contents' and @name != 'path' and @name != 'user']" />
                </entry>
            </xsl:for-each>
        </feed>
    </xsl:template>
</xsl:stylesheet>
