<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    xmlns:bib="http://bibtexml.sf.net/"
    exclude-result-prefixes="xs"
    version="2.0">
    <xsl:output
        method="xml" 
        doctype-public="-//Carnegie Mellon University//DTD Workbook Page 3.8//EN"
        doctype-system="http://oli.web.cmu.edu/dtd/oli_workbook_page_3_8.dtd"
        indent="yes"/>
    
    <!--Setting up to find the LO file-->
    <xsl:variable name="fileid" select="workbook_page/@id"/>
    <xsl:variable name="lofile" select="concat('LOs_',$fileid,'.xml')"/>
    <!--File that maps the EdX file names to the new OLI file names. 
        At the moment must be in the same directory as xsl file.-->
    <xsl:variable name="pagestable" select="'pagesTable.xml'"/>
    
    <!--I don't want to miss anything, so adding the identity transformation to by default copy everything
        (and be overidden by more specific templates for things that need different handling)-->
    <xsl:template match="@* | node()">
        <xsl:copy>
            <xsl:apply-templates select="@* | node()"/>
        </xsl:copy>
    </xsl:template>
    
    <!--pulling in the learning objectives from their file. 
        Note, atm LO file needs to be in the same directory as the xsl document.
        https://www.xml.com/pub/a/2002/03/06/xslt.html-->
    <xsl:template match="workbook_page/head">
        <head>
            <xsl:apply-templates select="@* | node()"/>
            <xsl:if test="document($lofile)">
                <xsl:apply-templates select="document($lofile)/objectives/objective"/>
            </xsl:if>  
        </head>
    </xsl:template>
    <xsl:template match="objective">
        <objref>
            <xsl:attribute name="idref">
                <xsl:value-of select="./@id"/>
            </xsl:attribute>
        </objref>
    </xsl:template>
    
    <!--were-h1s will be put in comments - don't want them in the output because the page title was already generated from display name, 
        but want them available because the two don't always match and this way they are easily available to compare-->
    <xsl:template match="h1">
        <xsl:text disable-output-escaping="yes">&lt;!--</xsl:text><h1><xsl:value-of select="."/></h1>--<xsl:text disable-output-escaping="yes">&gt;</xsl:text> <!--Trying to figure out how to do this. &lt;!- etc generates that same thing in output, which I don't want. From https://stackoverflow.com/questions/3932152/output-element-in-comments tried the xsl:comment thing but that gets rid of the h1 tags. From same source found this, which works!--> 
    </xsl:template>
    <!--generated-from-htmls-sections headed by h1s should actually not be sections, since the h1 is just the page title, not a section on the page-->
    <xsl:template match="section[h1]">
        <xsl:apply-templates select="@* | node()"/>
    </xsl:template>
    
    <!--sections were already generated so just need to change headers to the titles of those sections-->
    <!--MAY WANT TO REVISE TO ACCOUNT FOR POSSIBILITY OF MULTIPLE TITLES IN A SECTION-->
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
    
    <!--there's at least one page where the first part has both an h1 and an h2 for unclear reasons. This is to handle that case-->
        <!--section is treated like an h1 section; that is it should not be a section-->
    <xsl:template match="section[h1 and h2]" priority="1">
        <xsl:apply-templates select="@* | node()"/>
    </xsl:template>
        <!--the h2 is commented out like the h1-->
    <xsl:template match="section[h1 and h2]/h2" priority="1">
        <xsl:text disable-output-escaping="yes">&lt;!--</xsl:text><h2><xsl:value-of select="."/></h2>--<xsl:text disable-output-escaping="yes">&gt;</xsl:text> 
    </xsl:template>
    <!--there's also at least one page where the source had empty headers, leading to sections with empty titles and bodies by default processing, which is invalid. 
        Instead they should be removed.-->
    <xsl:template match="section[count(*)=1 and h2[matches(text(),'\s*')]|h3[matches(text(),'\s*')]|h4[matches(text(),'\s*')]|h5[matches(text(),'\s*')]|h6[matches(text(),'\s*')]]" priority="1"></xsl:template> <!--'has one child (so only the header) and the header is either empty or has just whitespace (in case that comes up)-->
    
    <!--Get rid of the old LO section, since LOs get put in differently-->
    <xsl:template match="div[descendant::li[matches(text(),'LO WAS HERE')]]"></xsl:template>
    
    <!--don't think we need these since we do our own styling-->
    <xsl:template match="link[@rel='stylesheet']"></xsl:template>
    <xsl:template match="@style"></xsl:template>
    <!--these seem to generally have their contents commented out already?-->
    <xsl:template match="style"></xsl:template>
    <!--colgroups (in tables) are only for styling (if I understand correctly), which we don't allow, so removing-->
    <xsl:template match="colgroup"></xsl:template>
    
    <!--bold and italics-->
    <xsl:template match="strong | b">
        <em style="bold"><xsl:apply-templates select="@* | node()"/></em>
    </xsl:template>
    <xsl:template match="i | em"> <!--POSSIBLY CHECK note, we also allow a plain em tag but it looks like it produces bold italics and html em is usually italics?-->
        <em style="italic"><xsl:apply-templates select="@* | node()"/></em>
    </xsl:template>
    
    <!--blockquote-->
    <xsl:template match="blockquote">
        <quote style="block"><xsl:apply-templates select="@* | node()"/></quote>
    </xsl:template>
    
    <!--links that jump to other course pages (use the pagestable to find the correct oli id of the page)-->
    <xsl:template match="a[matches(@href,'/jump_to_id/[a-z0-9]+')]">
        <xsl:variable name="pagetarget" select="tokenize(./@href,'/')[last()]"/>
        <xsl:variable name="olipageid" select="document($pagestable)/pages/page[@filename=$pagetarget]/id/@pageid"/> <!--in a variable since I'll use it twice-->
        <activity_link>
            <xsl:attribute name="idref">
                <xsl:value-of select="$olipageid"/>
            </xsl:attribute>
            <xsl:attribute name="title"> <!--for accessibility-->
                <xsl:value-of select="$olipageid"/>
            </xsl:attribute>
            <xsl:apply-templates select="@*[name()!='href' and name()!='title'] | node()"/> <!--the internal links I saw didn't have titles but in case some do-->
        </activity_link>
    </xsl:template>
    <!--external links-->
    <!--<xsl:template match="a">
        <link>
            --><!--<xsl:if test="./@target='[object Object]'"> 
                <xsl:attribute name="target"><xsl:text>new</xsl:text></xsl:attribute>
            </xsl:if> 
            <xsl:apply-templates select="@*[name()!='target'] | node()"/>--><!--
            <xsl:attribute name="href">
                <xsl:value-of select="./@href"/>
            </xsl:attribute>
            <xsl:if test="./@title"> 
                <xsl:attribute name="title"><xsl:value-of select="./@title"/></xsl:attribute> 
            </xsl:if> 
            <xsl:if test="./@target='[object Object]'">
                <xsl:attribute name="target"><xsl:text>new</xsl:text></xsl:attribute> 
            </xsl:if> 
            <xsl:apply-templates select="@* | node()"/>
        </link>
    </xsl:template>-->
    
    <!--bibliography-->
        <!--the bottom-of-page part-->
            <!--the reference section should be turned into the bib file-->
    <xsl:template match="section[h2[matches(text(),'Reference')]]" priority="1"><!--priority is so there is no ambiguous match with the general section handler-->
        <bib:file>
            <xsl:apply-templates select="./descendant::li"/>
        </bib:file>
    </xsl:template>
            <!--each work cited should be a bib entry-->
    <xsl:template match="section[h2[matches(text(),'Reference')]]//li" priority="1">
        <bib:entry>
            <xsl:attribute name="id"><xsl:value-of select="./a[1]/@name"/></xsl:attribute>
            <!--not currently trying to sort out the pieces/format of the works cited, so they're all misc->note-->
            <bib:misc>
                <bib:note>
                    <xsl:apply-templates select="@* | node()"/>
                </bib:note>
            </bib:misc>
        </bib:entry>
    </xsl:template>
            <!--handling contents of the works cited li's:
               -we want to delete span and its contents, since that's the 'return to text above' piece and we don't want it
               -we want to delete the first a since that was basically just holding the id and we handled it already
               -other a's should have their href content also printed in case the url was not included in text
               -all other descendants we want the bottom-out text of and nothing else
               (the specific ones have higher priority than the general descendent one so they fire when needed)-->
    <xsl:template match="section[h2[matches(text(),'Reference')]]//li/span" priority="1"/>
    <xsl:template match="section[h2[matches(text(),'Reference')]]//li/a[1]" priority="1"/>
    <xsl:template match="section[h2[matches(text(),'Reference')]]//li/a[position()>1]" priority="1">
        <xsl:apply-templates/>
        <xsl:text> [</xsl:text><xsl:value-of select="./@href"/><xsl:text>]</xsl:text>
    </xsl:template>
    <xsl:template match="section[h2[matches(text(),'Reference')]]//li//*">
        <xsl:apply-templates/>
    </xsl:template>
        <!--in-text citations-->
    <xsl:template match ="sup[span[a[@alt='Click to view full citation']]]"> <!--need a way to catch the citations. Need to check if this works enough.-->
        <cite>
            <xsl:attribute name="entry"><xsl:value-of select="substring(./span/a/@href,2)"/></xsl:attribute> <!--need to strip out the #-->
            <xsl:attribute name="title"><xsl:value-of select="./span/@title"/></xsl:attribute> <!--keeping it since it's there and helps accessibility-->
        </cite>
    </xsl:template>    
    
<!--Some quickpass things - TO ACTUALLY HANDLE LATER-->
    <xsl:template match="*[local-name()='math']"> <!--handling namespace problem: https://stackoverflow.com/questions/5239685/xml-namespace-breaking-my-xpath-->
        <xsl:text disable-output-escaping="yes">&lt;!--</xsl:text>
            <math>
                <xsl:apply-templates select="@* | node()"/>   
            </math>--<xsl:text disable-output-escaping="yes">&gt;</xsl:text> 
    </xsl:template>
    <xsl:template match="script">
        <xsl:text disable-output-escaping="yes">&lt;!--</xsl:text>
        <script>
            <xsl:apply-templates select="@* | node()"/>   
        </script>--<xsl:text disable-output-escaping="yes">&gt;</xsl:text> 
    </xsl:template>  
    <xsl:template match="li[@class='done']" priority =".7"> <!-- greater than .5 but below so def won't clash with bib stuff etc-->
        <li><xsl:text>✔ </xsl:text>
            <xsl:apply-templates select="@*[name()!='class'] | node()"/> 
        </li>
    </xsl:template>
    <xsl:template match="li[@class='notdone']" priority =".7">
        <li><xsl:text>✗ </xsl:text>
            <xsl:apply-templates select="@*[name()!='class'] | node()"/> 
        </li>
    </xsl:template>
    <xsl:template match="a">
        <link>
            <xsl:apply-templates select="@href | @title | node()"/> 
        </link>
    </xsl:template>
    <xsl:template match="img">
        <image>
            <xsl:attribute name="src"><xsl:value-of select="concat('..',replace(./@src,'static','webcontent'))"/></xsl:attribute>
            <xsl:apply-templates select="@alt | @title | @height | @width | node()"/> 
        </image>
    </xsl:template>

<!--Some further quickpass things - TO HANDLE BETTER LATER (these come out of attempting to mass process the entire content presentation chapter pages content with existing xslt)-->
    <!--sections with no headers. Curently made into not sections.-->
    <xsl:template match="section[not(h1) and not(h2) and not(h3) and not(h4) and not(h5) and not(h6)]">
        <xsl:apply-templates select="@* | node()"/> 
    </xsl:template>
    <!--badly nested lists-->
    <xsl:template match="ul/ul | ol/ul">
        <li>
            <ul>
                <xsl:apply-templates/> <!--later will need to change to  select="@* | node()" and handle attributes-->
            </ul>
        </li>
    </xsl:template>
    <xsl:template match="ul/ol | ol/ol">
        <li>
            <ol>
                <xsl:apply-templates/> <!--later will need to change to  select="@* | node()" and handle attributes-->
            </ol>
        </li>
    </xsl:template>
    <!--currently just being erased as tags. Will need to be improved: divs and spans might have functional attributes (some divs seem to have hidden), and br's are functional-->
    <xsl:template match="div|span|br|center">
        <xsl:apply-templates/> 
    </xsl:template>
    <!--list tags having not-permitted attributes stripped out (want to go through and handle better later, probably when have all the pages)-->
    <xsl:template match="li[@*]">
        <li><xsl:apply-templates select="@title | node()"/></li>
    </xsl:template>
    <xsl:template match="ul[@*]">
        <ul><xsl:apply-templates select="@title | node()"/></ul>
    </xsl:template>
    <xsl:template match="ol[@*]">
        <ol><xsl:apply-templates select="@title | node()"/></ol>
    </xsl:template>
    <!--currently being erased-->
    <xsl:template match="@align"></xsl:template>
    <xsl:template match="@class"></xsl:template>
    <!--currently being commented out-->
    <xsl:template match="g">
        <xsl:text disable-output-escaping="yes">&lt;!--</xsl:text>
        <g>
            <xsl:apply-templates select="@* | node()"/> 
        </g>--<xsl:text disable-output-escaping="yes">&gt;</xsl:text> 
    </xsl:template>
    <!--list tags having not-permitted attributes stripped out (want to go through and handle better later, probably when have all the pages)-->
        <!--additional note, unconventional_examples.xml, which has iframes, also has download links. Those go through fine but we may want to do something about them since they're not our servers.-->
    <xsl:template match="iframe">
        <iframe><xsl:apply-templates select="@src | @height | @width | node()"/></iframe>
    </xsl:template>
    <!--table pieces currently having their attributes all stripped out-->
    <xsl:template match="table"><table><xsl:apply-templates/></table></xsl:template>
    <xsl:template match="tr"><tr><xsl:apply-templates/></tr></xsl:template>
    <xsl:template match="th"><th><xsl:apply-templates/></th></xsl:template>
    <xsl:template match="td"><td><xsl:apply-templates/></td></xsl:template>
    <!--we don't have tbody-->
    <xsl:template match="tbody"><xsl:apply-templates/></xsl:template>
    
    <!--having an issue with bare text inside section bodies that used to be in divs-->
    <xsl:template match="div/text()[matches(.,'\S')]">
        <p>
            <xsl:value-of select="."/>
        </p>
    </xsl:template>
    
    <!--blockquotes don't allow paragraphs. At the moment making it a notation pullout; will consider more later-->
    <xsl:template match="blockquote[p]">
        <pullout type="notation"><xsl:apply-templates select="@* | node()"/></pullout>
    </xsl:template>
    <!--had an issue like the bare text one, except it used to be in a span in a div, with a bold-->
        <!--bold and italics-->
    <xsl:template match="div/strong | div/b | div/span/strong | div/span/b">
        <p><em style="bold"><xsl:apply-templates select="@* | node()"/></em></p>
    </xsl:template>
    <xsl:template match="div/i | div/em | div/span/i | div/span/em"> <!--POSSIBLY CHECK note, we also allow a plain em tag but it looks like it produces bold italics and html em is usually italics?-->
        <p><em style="italic"><xsl:apply-templates select="@* | node()"/></em></p>
    </xsl:template>
    <!--basically table titles-->
    <xsl:template match="div[h2 and div/table]">
        <table>
            <title><xsl:value-of select="./h2"/></title>
            <xsl:apply-templates select="./descendant::table/*"/> <!--not doing the attributes since table attributes are currently being stripped out in general-->
        </table>
    </xsl:template>

<!--Some further quickpass things - TO HANDLE BETTER LATER (from trying to process chapter course structure)-->
    <!--we do not allow empty tr's-->
    <xsl:template match="tr[not(th) and not(td)]"/>
</xsl:stylesheet>