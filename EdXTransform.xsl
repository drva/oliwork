<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    exclude-result-prefixes="xs"
    version="2.0">
    <xsl:output
        method="xml" 
        doctype-public="-//Carnegie Mellon University//DTD Workbook Page 3.8//EN"
        doctype-system="http://oli.web.cmu.edu/dtd/oli_workbook_page_3_8.dtd"
        indent="yes"/>
    
    <xsl:template match="/">
        <workbook_page>
            <xsl:apply-templates/>
        </workbook_page>
    </xsl:template>
    
    <!--keep comments-->
    <xsl:template match="comment()">
        <xsl:copy/>
    </xsl:template>
    
    <!--were-h1s will be put in comments - don't want them in the output because the page title was already generated from display name, 
        but want them available because the two don't always match and this way they are easily available to compare-->
    <xsl:template match="h1">
        <xsl:text disable-output-escaping="yes">&lt;!--</xsl:text><h1><xsl:value-of select="."/></h1>--<xsl:text disable-output-escaping="yes">&gt;</xsl:text> <!--Trying to figure out how to do this. &lt;!- etc generates that same thing in output, which I don't want. From https://stackoverflow.com/questions/3932152/output-element-in-comments tried the xsl:comment thing but that gets rid of the h1 tags. From same source found this, which works!--> 
    </xsl:template> 
    
    <!--sections were already generated so just need to change headers to the titles of those sections-->
    <xsl:template match="h2|h3|h4|h5|h6">
        <title><xsl:value-of select="."/></title>
    </xsl:template>
    <!--and put in bodies after the titles-->
    <xsl:template match="section[h2|h3|h4|h5|h6]">
        <section>
            <xsl:apply-templates select="h2|h3|h4|h5|h6"/>
            <body>
                <xsl:apply-templates select="*[not(self::h2 or self::h3 or self::h4 or self::h5 or self::h6)]"/>
            </body>
        </section> 
    </xsl:template>
    
    <xsl:template match="title|head|body|p|ol|ul|li">
        <xsl:copy copy-namespaces="no">
            <xsl:apply-templates/>
        </xsl:copy>
    </xsl:template>
    
    <xsl:template match="codeblock">
        <xsl:copy copy-namespaces="no">
            <xsl:apply-templates/>
        </xsl:copy>
    </xsl:template>
    
</xsl:stylesheet>