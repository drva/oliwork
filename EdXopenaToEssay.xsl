<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    exclude-result-prefixes="xs"
    version="2.0">
    <xsl:output
        method="xml" 
        doctype-public="-//Carnegie Mellon University//DTD Assessment MathML 2.4//EN"
        doctype-system="http://oli.web.cmu.edu/dtd/oli_assessment_mathml_2_4.dtd"
        indent="yes"/>
       
    <!--I don't want to miss anything, so adding the identity transformation to by default copy everything
        (and be overidden by more specific templates for things that need different handling)-->
    <xsl:template match="@* | node()">
        <xsl:copy>
            <xsl:apply-templates select="@* | node()"/>
        </xsl:copy>
    </xsl:template>
    
    <xsl:template match="openassessment"> <!--Currently not handling most of the attributes-->
        <xsl:result-document href="{concat('sa_',./@url_name, '.xml')}"> <!--https://www.oxygenxml.com/forum/topic7987.html-->
            <assessment>
                <xsl:attribute name="id"><xsl:value-of select="concat('sa_',./@url_name)"/></xsl:attribute>
                <xsl:apply-templates select="title"/> <!--I think this'll get the title bc of the identity transformation?-->
                <essay id="{concat('saQ_',./@url_name)}" grading="instructor"> <!--this means I can only make essays atm but I don't think I currently have a better way to do this-->
                    <xsl:apply-templates select="*[not(self::title)]"/> <!--since already did title-->
                </essay>
            </assessment>
        </xsl:result-document>
    </xsl:template>
    
    <xsl:template match="assessments"/> <!--don't think I need anything from here; getting rid of-->
    
    <!--prompts/prompt/description is the question body; doing by sending through until the right level-->
    <xsl:template match="prompts">
        <xsl:apply-templates/>
    </xsl:template>
    <xsl:template match="prompt">
        <xsl:apply-templates/>
    </xsl:template>
    <xsl:template match="prompt/description">
        <body>
            <xsl:apply-templates/>
        </body>
    </xsl:template>
    
    <xsl:template match="rubric"> <!--sending through-->
        <xsl:apply-templates/>
    </xsl:template>
    <xsl:template match="criterion"> <!--TODO one of the criterions has an attribute feedback="optional"; need to decide what to do with-->
       <part>
           <title>
               <!--Since we only have title here, I'm combining the pieces they have into it
               In the current examples I have from KTH#2 name and label have the same value, so I only want one of them, but if they differed would make sense to have both-->
               <xsl:choose>
                   <xsl:when test="./name = ./label">
                       <xsl:value-of select="concat(./name, ': ', ./prompt)"/>
                   </xsl:when>
                   <xsl:otherwise>
                       <xsl:value-of select="concat(./name, ', ', ./label, ': ', ./prompt)"/>
                   </xsl:otherwise>
               </xsl:choose>
           </title>
           <xsl:apply-templates select="*[not(self::name or self::label or self::prompt)]"/> <!--again excluding the things I already handled-->
       </part>
    </xsl:template>
    
    <xsl:template match="criterion/option">
        <grading_criteria>
            <xsl:attribute name="score"><xsl:value-of select="./@points"/></xsl:attribute>
            <p>
            <!--again combining pieces, with provision for name and label not always matching-->
            <xsl:choose>
                <xsl:when test="./name = ./label">
                    <xsl:value-of select="concat(./name, ': ', ./explanation)"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="concat(./name, ', ', ./label, ': ', ./explanation)"/>
                </xsl:otherwise>
            </xsl:choose>
            </p>
        </grading_criteria>
    </xsl:template>
    
    <!--they have a <feedbackprompt> and a <feedback_default_text> (which are not nested) seperate from criterion; trying to come up with a way to get in our system-->
    <xsl:template match="feedbackprompt">
        <part score_out_of="0">
            <title>Feedback prompt</title>
            <grading_criteria score="0">
                <p><xsl:apply-templates/></p> <!--to get the text of the feedbackprompt element in here-->
                <feedback><xsl:text>Default text: </xsl:text><xsl:value-of select ="//feedback_default_text/text()"/></feedback>
            </grading_criteria>
        </part>
    </xsl:template> 
    <xsl:template match="feedback_default_text"/> <!--grabbing this a different way so here taking it out-->
    
    <xsl:template match="br"> <!--just stripping it out meant some words had no whitespace between them at all, so replacing with a space, atm-->
        <xsl:text> </xsl:text>
    </xsl:template>
    
</xsl:stylesheet>